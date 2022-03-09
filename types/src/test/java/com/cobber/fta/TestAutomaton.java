/*
 * Copyright 2017-2022 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

public class TestAutomaton {

	boolean plausible(String input, State state) {
		// Get a list of transitions ordered by (min, reverse max, to)
		List<Transition> transitions = state.getSortedTransitions(false);

		if (input.isEmpty() && state.isAccept())
			return true;

		if (transitions.size() == 0)
            return input.isEmpty();

		char ch = input.charAt(0);
		for (Transition transition : transitions)
			if (ch >= transition.getMin() && ch <= transition.getMax())
				return plausible(input.substring(1), transition.getDest());

		return false;
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testSimple() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]:").toAutomaton();
		String input = "7:";
		assertTrue(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testExcess() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]:").toAutomaton();
		String input = "7: ";
		assertFalse(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testCounting() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]{2,3}").toAutomaton();
		String input = "79";
		assertTrue(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testCountingExcess() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]{2,3}").toAutomaton();
		String input = "7979";
		assertFalse(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testTwoBranch() throws IOException, FTAException {
		Automaton automaton = new RegExp("([0-6]=)|([7-9]:)").toAutomaton();
		String input = "7:";
		assertTrue(plausible(input, automaton.getInitialState()));
	}
}
