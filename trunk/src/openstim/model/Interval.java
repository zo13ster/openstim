
package openstim.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class Interval {
	public float t_attack;
	public float t_on;
	public float t_release;
	public float t_off;

	/**
	* Default constructor.
	*/

	public Interval() {
		t_attack = 1.0f;
		t_on = 9.0f;
		t_release = 0.0f;
		t_off = 0.0f;
		normalize();
	}

	/**
	* Copy constructor.
	*/

	public Interval(Interval other) {
		t_attack = other.t_attack;
		t_on = other.t_on;
		t_release = other.t_release;
		t_off = other.t_off;
		normalize();
	}

	/**
	* Interpolation constructor.
	*/

	public Interval(float s1, Interval i1, float s2, Interval i2) {
		interpolate(s1, i1, s2, i2);
	}

	public void interpolate(float s1, Interval i1, float s2, Interval i2) {
		for (int i = 0; i < params.length; i++) {
			setParam(i, s1 * i1.getParam(i) + s2 * i2.getParam(i));
		}
		normalize();
	}

	/**
	* Labels for the parameters of the interval.
	* This is used by the dialog for creating/modifying an interval.
	*/

	public static final String[] params = { "Attack", "On", "Release", "Off" };

	/**
	* Allow anonymous access to the parameters of the interval.
	* This is used by the dialog for creating/modifying an interval.
	*/

	public float getParam(int i) {
		switch (i) {
			case 0  : return t_attack;
			case 1  : return t_on;
			case 2  : return t_release;
			case 3  : return t_off;
			default : return 0.0f;
		}
	}

	/**
	* Allow anonymous access to the parameters of the interval.
	* This is used by the dialog for creating/modifying an interval.
	*/

	public void setParam(int i, float v) {
		switch (i) {
			case 0 : t_attack = v; break;
			case 1 : t_on = v; break;
			case 2 : t_release = v; break;
			case 3 : t_off = v; break;
		}
	}

	/**
	* Sanitize interval parameters.
	*/

	public void normalize() {
		t_attack = Math.max(0.0f, t_attack);
		t_on = Math.max(0.0f, t_on);
		t_release = Math.max(0.0f, t_release);
		t_off = Math.max(0.0f, t_off);
	}

	public Element toXML(Document doc) {
		Element xml = doc.createElement("interval");
		xml.setAttribute("attack", Float.toString(t_attack));
		xml.setAttribute("on", Float.toString(t_on));
		xml.setAttribute("release", Float.toString(t_release));
		xml.setAttribute("off", Float.toString(t_off));
		return xml;
	}

	private int state_phase = 0;
	private float state_time = 0.0f;

	public void reset() {
		state_phase = 0;
		state_time = 0.0f;
	}

	public float nextValue(float step) {
		if (state_phase == 1 && t_release < Float.MIN_VALUE && t_off < Float.MIN_VALUE) return 1.0f;
		if (state_phase == 3 && t_attack < Float.MIN_VALUE && t_on < Float.MIN_VALUE) return 0.0f;
		state_time += step;
		while (true) {
			if (state_phase == 0) {
				if (state_time > t_attack || t_attack < Float.MIN_VALUE) {
					state_phase = 1;
					state_time -= t_attack;
				} else {
					return state_time / t_attack;
				}
			} else if (state_phase == 1) {
				if (state_time > t_on || t_on < Float.MIN_VALUE) {
					state_phase = 2;
					state_time -= t_on;
				} else {
					return 1.0f;
				}
			} else if (state_phase == 2) {
				if (state_time > t_release || t_release < Float.MIN_VALUE) {
					state_phase = 3;
					state_time -= t_release;
				} else {
					return 1.0f - state_time / t_release;
				}
			} else {
				if (state_time > t_off || t_off < Float.MIN_VALUE) {
					state_phase = 0;
					state_time -= t_off;
				} else {
					return 0.0f;
				}
			}
		}
	}

	/*public static final class State {
		public int phase;
		public float time;

		public State() {
			phase = 0;
			time = 0.0f;
		}
	}*/
}



