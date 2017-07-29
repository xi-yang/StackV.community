package net.maxgigapop.mrs.driver.ccsn;

class ThreeTuple<F extends Comparable<?>, S extends Comparable<?>, T extends Comparable<?>>
implements Comparable<ThreeTuple<F, S, T>> {
	private F first;
	private S second;
	private T third;
	
	public
	enum Term {
		FIRST, SECOND, THIRD;
	}

	public
	ThreeTuple(F first, S second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public
	Object get(Term term) {
		Object returned = null;
		switch (term) {
		case FIRST:
			returned = first;
		case SECOND:
			returned = second;
		case THIRD:
			returned = third;
		}
		return returned;
	}
	
	@Override
	public
	boolean equals(Object other) {
		boolean isEqual = false;
		
		check:
		{
			if (!(other instanceof ThreeTuple))
				break check;
			
			@SuppressWarnings("unchecked")
			ThreeTuple<F, S, T> otherTuple = (ThreeTuple<F, S, T>) other;
			if (!first.equals(otherTuple.first))
				break check;
			if (!second.equals(otherTuple.second))
				break check;
			
			isEqual = true;
		}
		
		return isEqual;
	}
	
	@Override
	public
	int hashCode() {
		return first.hashCode() + 2*second.hashCode();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public
	int compareTo(ThreeTuple<F, S, T> o) {
		int rc = 0;
		Comparable<F> o_first = (Comparable<F>) o.get(Term.FIRST);
		Comparable<S> o_second = (Comparable<S>) o.get(Term.SECOND);
		Comparable<T> o_third = (Comparable<T>) o.get(Term.THIRD);
		
		compare:
		{
			if ( o_first.compareTo(this.first) < 0 ) {
				rc = -1;
				break compare;
			} else if ( o_first.compareTo(this.first) > 0) {
				rc = 1;
				break compare;
			}
			if ( o_second.compareTo(this.second) < 0 ) {
				rc = -1;
				break compare;
			} else if ( o_second.compareTo(this.second) > 0) {
				rc = 1;
				break compare;
			}
			if ( o_third.compareTo(this.third) < 0 )
				rc = -1;
			else if ( o_third.compareTo(this.third) > 0)
				rc = 1;
		}
		
		return rc;
	}

	@Override
	public
	String toString() {
		return "{" + first + " : " + second + '}';
	}
}
