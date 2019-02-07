package com.cobber.fta;

public class RegExpLength {
	int min;
	int max;
	int length;

	RegExpLength(int min, int max) {
		this.min = min;
		this.max = max;
	}

	RegExpLength() {
		this(0, 0);
	}

	@Override
	public String toString() {
		return Utils.regExpLength(min, max);
	}
}
