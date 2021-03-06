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

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.log.LogHP;
import org.rapidoid.util.Constants;
import org.rapidoid.util.U;
import org.rapidoidx.net.Protocol;
import org.rapidoidx.net.abstracts.Channel;

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public abstract class FiniteStateProtocol implements Constants, Protocol {

	private static final int MAX_STATES = 20;

	protected static final int STOP = -1;

	private final int statesCount;

	public FiniteStateProtocol(int statesCount) {
		U.must(statesCount > 0 && statesCount < MAX_STATES, "Unsupported number of states: %s", statesCount);
		this.statesCount = statesCount;
	}

	@Override
	public final void process(Channel ctx) {
		ConnState state = ctx.state();

		U.must(state.n != STOP, "The protocol was terminated!");

		U.must(state.n >= 0 && state.n < statesCount, "Invalid state number!");

		int stateN = (int) state.n;

		int nextState;

		switch (stateN) {

		case 0:
			nextState = state0(ctx);
			break;

		case 1:
			nextState = state1(ctx);
			break;

		case 2:
			nextState = state2(ctx);
			break;

		case 3:
			nextState = state3(ctx);
			break;

		case 4:
			nextState = state4(ctx);
			break;

		case 5:
			nextState = state5(ctx);
			break;

		case 6:
			nextState = state6(ctx);
			break;

		case 7:
			nextState = state7(ctx);
			break;

		case 8:
			nextState = state8(ctx);
			break;

		case 9:
			nextState = state9(ctx);
			break;

		case 10:
			nextState = state10(ctx);
			break;

		case 11:
			nextState = state11(ctx);
			break;

		case 12:
			nextState = state12(ctx);
			break;

		case 13:
			nextState = state13(ctx);
			break;

		case 14:
			nextState = state14(ctx);
			break;

		case 15:
			nextState = state15(ctx);
			break;

		case 16:
			nextState = state16(ctx);
			break;

		case 17:
			nextState = state17(ctx);
			break;

		case 18:
			nextState = state18(ctx);
			break;

		case 19:
			nextState = state19(ctx);
			break;

		default:
			throw U.rte("Cannot handle state: " + stateN);
		}

		if (state.n == STOP) {
			LogHP.debug("Terminating protocol", "from", state.n);
		} else {
			LogHP.debug("Switching protocol state", "from", state.n, "to", nextState);
		}

		state.n = nextState;
	}

	protected int state0(Channel ctx) {
		throw U.rte("State 0 handler is not implemented!");
	}

	protected int state1(Channel ctx) {
		throw U.rte("State 1 handler is not implemented!");
	}

	protected int state2(Channel ctx) {
		throw U.rte("State 2 handler is not implemented!");
	}

	protected int state3(Channel ctx) {
		throw U.rte("State 3 handler is not implemented!");
	}

	protected int state4(Channel ctx) {
		throw U.rte("State 4 handler is not implemented!");
	}

	protected int state5(Channel ctx) {
		throw U.rte("State 5 handler is not implemented!");
	}

	protected int state6(Channel ctx) {
		throw U.rte("State 6 handler is not implemented!");
	}

	protected int state7(Channel ctx) {
		throw U.rte("State 7 handler is not implemented!");
	}

	protected int state8(Channel ctx) {
		throw U.rte("State 8 handler is not implemented!");
	}

	protected int state9(Channel ctx) {
		throw U.rte("State 9 handler is not implemented!");
	}

	protected int state10(Channel ctx) {
		throw U.rte("State 10 handler is not implemented!");
	}

	protected int state11(Channel ctx) {
		throw U.rte("State 11 handler is not implemented!");
	}

	protected int state12(Channel ctx) {
		throw U.rte("State 12 handler is not implemented!");
	}

	protected int state13(Channel ctx) {
		throw U.rte("State 13 handler is not implemented!");
	}

	protected int state14(Channel ctx) {
		throw U.rte("State 14 handler is not implemented!");
	}

	protected int state15(Channel ctx) {
		throw U.rte("State 15 handler is not implemented!");
	}

	protected int state16(Channel ctx) {
		throw U.rte("State 16 handler is not implemented!");
	}

	protected int state17(Channel ctx) {
		throw U.rte("State 17 handler is not implemented!");
	}

	protected int state18(Channel ctx) {
		throw U.rte("State 18 handler is not implemented!");
	}

	protected int state19(Channel ctx) {
		throw U.rte("State 19 handler is not implemented!");
	}

}
