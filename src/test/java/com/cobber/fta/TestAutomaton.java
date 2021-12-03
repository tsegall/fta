package com.cobber.fta;

import java.io.IOException;
import java.util.List;

import org.testng.Assert;
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
		Assert.assertTrue(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testExcess() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]:").toAutomaton();
		String input = "7: ";
		Assert.assertFalse(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testCounting() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]{2,3}").toAutomaton();
		String input = "79";
		Assert.assertTrue(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testCountingExcess() throws IOException, FTAException {
		Automaton automaton = new RegExp("[0-9]{2,3}").toAutomaton();
		String input = "7979";
		Assert.assertFalse(plausible(input, automaton.getInitialState()));
	}

	@Test(groups = { TestGroups.ALL, "automaton" })
	public void testTwoBranch() throws IOException, FTAException {
		Automaton automaton = new RegExp("([0-6]=)|([7-9]:)").toAutomaton();
		String input = "7:";
		Assert.assertTrue(plausible(input, automaton.getInitialState()));
	}
}
