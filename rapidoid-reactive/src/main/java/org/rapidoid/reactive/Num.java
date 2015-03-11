package org.rapidoid.reactive;

public interface Num {

	int get();

	Num plus(Num n);

	Num minus(Num n);

	Num mul(Num n);

	Num div(Num n);

	Bool eq(Num n);

	Num abs();

}
