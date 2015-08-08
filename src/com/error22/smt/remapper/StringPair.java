package com.error22.smt.remapper;

public class StringPair {
	private String a, b;

	public StringPair(String name, String desc) {
		this.a = name;
		this.b = desc;
	}

	public String getA() {
		return a;
	}

	public String getB() {
		return b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringPair other = (StringPair) obj;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FieldData [name=" + a + ", desc=" + b + "]";
	}

}
