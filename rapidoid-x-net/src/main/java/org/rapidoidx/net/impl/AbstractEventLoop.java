package org.rapidoidx.net.impl;

/*
 * #%L
 * rapidoid-x-net
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.log.Log;

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public abstract class AbstractEventLoop<T> extends AbstractLoop<T> {

	protected final Selector selector;

	public AbstractEventLoop(String name) {
		super(name);

		Selector sel;

		try {
			sel = Selector.open();
		} catch (IOException e) {
			Log.error("Cannot open selector!", e);
			throw new RuntimeException(e);
		}

		this.selector = sel;
	}

	private void processKey(SelectionKey key) {
		if (key == null || !key.isValid()) {
			return;
		}

		if (key.isAcceptable()) {
			Log.debug("accepting", "key", key);

			try {
				acceptOP(key);
			} catch (IOException e) {
				failedOP(key, e);
				Log.error("accept IO error for key: " + key, e);
			} catch (Throwable e) {
				failedOP(key, e);
				Log.error("accept failed for key: " + key, e);
			}

		} else if (key.isConnectable()) {
			Log.debug("connection event", "key", key);

			try {
				connectOP(key);
			} catch (IOException e) {
				failedOP(key, e);
				Log.error("connect IO error for key: " + key, e);
			} catch (Throwable e) {
				failedOP(key, e);
				Log.error("connect failed for key: " + key, e);
			}
		} else if (key.isReadable()) {
			Log.debug("reading", "key", key);

			try {
				readOP(key);
			} catch (IOException e) {
				failedOP(key, e);
				Log.error("read IO error for key: " + key, e);
			} catch (Throwable e) {
				failedOP(key, e);
				Log.error("read failed for key: " + key, e);
			}

		} else if (key.isWritable()) {
			Log.debug("writing", "key", key);

			try {
				writeOP(key);
			} catch (IOException e) {
				failedOP(key, e);
				Log.error("write IO error for key: " + key, e);
			} catch (Throwable e) {
				failedOP(key, e);
				Log.error("write failed for key: " + key, e);
			}
		}
	}

	@Override
	protected final void insideLoop() {

		try {
			doProcessing();
		} catch (Throwable e) {
			Log.error("Event processing error!", e);
		}

		try {
			selector.select(getSelectorTimeout());
		} catch (IOException e) {
			Log.error("Select failed!", e);
		}

		try {
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			synchronized (selectedKeys) {
				if (!selectedKeys.isEmpty()) {
					Iterator<?> iter = selectedKeys.iterator();

					while (iter.hasNext()) {
						SelectionKey key = (SelectionKey) iter.next();
						iter.remove();

						processKey(key);
					}
				}
			}
		} catch (ClosedSelectorException e) {
			// do nothing
		}
	}

	protected long getSelectorTimeout() {
		return 10;
	}

	protected abstract void doProcessing();

	protected void acceptOP(SelectionKey key) throws IOException {
		throw new RuntimeException("Accept operation is not implemented!");
	}

	protected void connectOP(SelectionKey key) throws IOException {
		throw new RuntimeException("Connect operation is not implemented!");
	}

	protected void readOP(SelectionKey key) throws IOException {
		throw new RuntimeException("Accept operation is not implemented!");
	}

	protected void writeOP(SelectionKey key) throws IOException {
		throw new RuntimeException("Accept operation is not implemented!");
	}

	protected void failedOP(SelectionKey key, Throwable e) {
		// ignore the errors by default
	}

}
