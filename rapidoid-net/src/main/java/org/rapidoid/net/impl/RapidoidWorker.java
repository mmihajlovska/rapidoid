package org.rapidoid.net.impl;

/*
 * #%L
 * rapidoid-net
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.buffer.BufGroup;
import org.rapidoid.buffer.IncompleteReadException;
import org.rapidoid.config.Conf;
import org.rapidoid.ctx.Ctxs;
import org.rapidoid.log.Log;
import org.rapidoid.net.Protocol;
import org.rapidoid.pool.Pool;
import org.rapidoid.pool.Pools;
import org.rapidoid.util.SimpleList;
import org.rapidoid.util.U;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class RapidoidWorker extends AbstractEventLoop<RapidoidWorker> {

	public static int MAX_IO_WORKERS = 1024;

	public static boolean EXTRA_SAFE = false;

	private final Queue<SocketChannel> connected;

	private final SimpleList<RapidoidConnection> done;

	private final Pool<RapidoidConnection> connections;

	private final int maxPipelineSize;

	final Protocol serverProtocol;

	final RapidoidHelper helper;

	private final int bufSize;

	private final boolean noDelay;

	private final BufGroup bufs;

	private volatile long messagesProcessed;

	RapidoidWorker next;

	public RapidoidWorker(String name, final Protocol protocol, final RapidoidHelper helper, int bufSizeKB,
			boolean noNelay) {
		super(name);

		this.bufs = new BufGroup(14); // 2^14B (16 KB per buffer segment)
		this.serverProtocol = protocol;
		this.helper = helper;

		this.maxPipelineSize = Conf.option("pipeline-max", Integer.MAX_VALUE);

		final int queueSize = Conf.micro() ? 1000 : 1000000;
		final int growFactor = Conf.micro() ? 2 : 10;

		this.connected = new ArrayBlockingQueue<SocketChannel>(queueSize);
		this.done = new SimpleList<RapidoidConnection>(queueSize / 10, growFactor);

		connections = Pools.create("connections", new Callable<RapidoidConnection>() {
			@Override
			public RapidoidConnection call() throws Exception {
				return newConnection();
			}
		}, 100000);

		this.bufSize = bufSizeKB * 1024;
		this.noDelay = noNelay;
	}

	public void accept(SocketChannel socketChannel) {
		connected.add(socketChannel);
		selector.wakeup();
	}

	private void configureSocket(SocketChannel socketChannel) throws IOException, SocketException {
		socketChannel.configureBlocking(false);

		Socket socket = socketChannel.socket();
		socket.setTcpNoDelay(noDelay);
		socket.setReceiveBufferSize(bufSize);
		socket.setSendBufferSize(bufSize);
		socket.setReuseAddress(true);
	}

	@Override
	protected void readOP(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		RapidoidConnection conn = (RapidoidConnection) key.attachment();

		int read;
		try {
			read = conn.input.append(socketChannel);
		} catch (Exception e) {
			read = -1;
		}

		if (read == -1) {
			// the other end closed the connection
			Log.debug("The other end closed the connection!");
			conn.closing = true;
		}

		process(conn);

		if (conn.closing) {
			close(key);
		}
	}

	public void process(RapidoidConnection conn) {
		messagesProcessed += processMsgs(conn);

		conn.completedInputPos = conn.input.position();
	}

	private long processMsgs(RapidoidConnection conn) {
		long reqN = 0;

		while (reqN < maxPipelineSize && conn.input().hasRemaining() && processNext(conn, false)) {
			reqN++;
		}

		return reqN;
	}

	private boolean processNext(RapidoidConnection conn, boolean initial) {

		if (initial) {
			conn.log("<< INIT >>");

			conn.requestId = -1;
		} else {
			conn.log("<< PROCESS >>");
			U.must(conn.input().hasRemaining());

			conn.requestId = helper.requestIdGen;
			helper.requestIdGen += MAX_IO_WORKERS;
			helper.requestCounter++;
		}

		// prepare for a rollback in case the message isn't complete yet
		conn.input().checkpoint(conn.input().position());
		int limit = conn.input().limit();
		int osize = conn.output().size();

		conn.input().setReadOnly(true);

		ConnState state = conn.state();
		long stateN = state.n;
		Object stateObj = state.obj;

		try {
			conn.done = false;

			if (EXTRA_SAFE) {
				processNextExtraSafe(conn);
			} else {
				conn.getProtocol().process(conn);
			}

			conn.input().setReadOnly(false);

			if (!conn.closed && !conn.isAsync()) {
				conn.done();
			}

			conn.input().deleteBefore(conn.input().checkpoint());

			// Log.debug("Completed message processing");
			return true;

		} catch (IncompleteReadException e) {

			// Log.debug("Incomplete message");
			conn.log("<< ROLLBACK >>");

			// input not complete, so rollback
			conn.input().position(conn.input().checkpoint());
			conn.input().limit(limit);
			conn.input().setReadOnly(false);

			conn.output().deleteAfter(osize);

			state.n = stateN;
			state.obj = stateObj;

		} catch (ProtocolException e) {

			conn.log("<< PROTOCOL ERROR >>");
			Log.warn("Protocol error", "error", e);
			conn.output().deleteAfter(osize);
			conn.write(U.or(e.getMessage(), "Protocol error!"));
			conn.error();
			conn.close(true);

		} catch (Throwable e) {

			conn.log("<< ERROR >>");
			Log.error("Failed to process message!", e);
			conn.close(true);
		}

		return false;
	}

	private void processNextExtraSafe(RapidoidConnection conn) {
		if (Ctxs.hasContext()) {
			Log.warn("Detected unclosed context before processing message!");
			Ctxs.close();
		}

		try {
			conn.getProtocol().process(conn);
		} finally {
			if (Ctxs.hasContext()) {
				Log.warn("Detected unclosed context after processing message!");
				Ctxs.close();
			}
		}
	}

	public void close(RapidoidConnection conn) {
		close(conn.key);
	}

	private void close(SelectionKey key) {
		try {
			Object attachment = key.attachment();

			clearKey(key);

			if (attachment instanceof RapidoidConnection) {
				RapidoidConnection conn = (RapidoidConnection) attachment;

				if (conn != null) {
					if (!conn.closed) {
						Log.trace("Closing connection", "connection", conn);
						assert conn.key == key;
						conn.reset();
						connections.release(conn);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearKey(SelectionKey key) throws IOException {
		if (key.isValid()) {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			socketChannel.close();
			key.attach(null);
			key.cancel();
		}
	}

	@Override
	protected void writeOP(SelectionKey key) throws IOException {
		RapidoidConnection conn = (RapidoidConnection) key.attachment();
		SocketChannel socketChannel = (SocketChannel) key.channel();

		checkOnSameThread();

		try {
			int wrote = conn.output.writeTo(socketChannel);
			conn.output.deleteBefore(wrote);

			boolean complete = conn.output.size() == 0;

			if (conn.closeAfterWrite() && complete) {
				close(conn);
			} else {
				if (complete) {
					key.interestOps(SelectionKey.OP_READ);
				} else {
					key.interestOps(SelectionKey.OP_READ + SelectionKey.OP_WRITE);
				}
				conn.wrote(complete);
			}
		} catch (IOException e) {
			close(conn);
		}
	}

	public void wantToWrite(RapidoidConnection conn) {
		if (onSameThread()) {
			conn.key.interestOps(SelectionKey.OP_WRITE);
		} else {
			wantToWriteAsync(conn);
		}
	}

	private void wantToWriteAsync(RapidoidConnection conn) {
		synchronized (done) {
			done.add(conn);
		}

		selector.wakeup();
	}

	@Override
	protected void doProcessing() {

		SocketChannel schannel;

		while ((schannel = connected.poll()) != null) {

			try {
				configureSocket(schannel);
			} catch (IOException e) {
				Log.error("Cannot configure channel!", e);
				continue;
			}

			RapidoidChannel channel = new RapidoidChannel(schannel, false, serverProtocol);

			SocketChannel socketChannel = channel.socketChannel;
			Log.debug("connected", "address", socketChannel.socket().getRemoteSocketAddress());

			try {
				SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ);
				U.notNull(channel.protocol, "protocol");
				RapidoidConnection conn = attachConn(newKey, channel.protocol);

				conn.setClient(channel.isClient);

				try {
					processNext(conn, true);
				} finally {
					conn.setInitial(false);
				}

			} catch (ClosedChannelException e) {
				Log.warn("Closed channel", e);
			}
		}

		synchronized (done) {
			for (int i = 0; i < done.size(); i++) {
				RapidoidConnection conn = done.get(i);
				if (conn.key != null && conn.key.isValid()) {
					conn.key.interestOps(SelectionKey.OP_WRITE);
				}
			}
			done.clear();
		}
	}

	private RapidoidConnection attachConn(SelectionKey key, Protocol protocol) {
		U.notNull(key, "protocol");
		U.notNull(protocol, "protocol");

		assert key.attachment() == null;

		RapidoidConnection conn = connections.get();

		// the connection is reset when closed
		// but a protocol can modify the connection after closing it
		// so it is reset again before reuse
		conn.reset();

		U.must(conn.closed);
		conn.closed = false;

		conn.key = key;
		conn.setProtocol(protocol);

		if (protocol instanceof CtxListener) {
			conn.setListener((CtxListener) protocol);
		}

		key.attach(conn);

		return conn;
	}

	@Override
	protected void failedOP(SelectionKey key, Throwable e) {
		Log.error("Network error", e);
		close(key);
	}

	public RapidoidConnection newConnection() {
		return new RapidoidConnection(RapidoidWorker.this, bufs);
	}

	public long getMessagesProcessed() {
		return messagesProcessed;
	}

}
