
package openstim.model;

import java.util.Arrays;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class Waveform {
	public static final int NUM_SHAPES  = 4;
	public static final int NUM_SAMPLES = 4096;

	public static enum Shape {
		NONE("(not used)"),
		SINE("Sine wave"),
		SQUARE("Square wave"),
		TRIANGLE("Triangle wave"),
		BIPOLAR("Bipolar wave"),
		UNIPOLAR("Unipolar wave"),
		SAWTOOTH("Sawtooth wave");

		private final String name;
		private Shape(String name) { this.name = name; }
		@Override public String toString() { return name; }

		static Shape fromString(String s) {
			for (Shape shape : values()) if (shape.name().equalsIgnoreCase(s)) return shape;
			return null;
		}
	}

	private final Shape[] shape;
	private final float[] weight;
	private final float[] speed;
	private final float[] phase;
	private final float[] samples;
	private boolean dirty;
	private String spec;

	/**
	* Default constructor.
	*/

	public Waveform() {
		shape = new Shape[NUM_SHAPES];
		weight = new float[NUM_SHAPES];
		speed = new float[NUM_SHAPES];
		phase = new float[NUM_SHAPES];
		samples = new float[NUM_SAMPLES];
		Arrays.fill(weight, 0.5f);
		shape[0] = Shape.SINE;
		weight[0] = 1.0f;
		dirty = true;
		spec = null;
	}

	/**
	* Copy constructor.
	*/

	public Waveform(Waveform other) {
		shape = Arrays.copyOf(other.shape, other.shape.length);
		weight = Arrays.copyOf(other.weight, other.weight.length);
		speed = Arrays.copyOf(other.speed, other.speed.length);
		phase = Arrays.copyOf(other.phase, other.phase.length);
		samples = new float[NUM_SAMPLES];
		dirty = true;
		spec = null;
	}

	/**
	* Set waveform parameters according to spec string.
	* @see #toString
	*/

	public void assign(String spec) throws Exception {
		String[] shapes = spec.toUpperCase().split(";");
		Arrays.fill(shape, Waveform.Shape.NONE);
		Arrays.fill(weight, 0.5f);
		Arrays.fill(speed, 0.0f);
		Arrays.fill(phase, 0.0f);
		weight[0] = 1.0f;
		dirty = true;
		spec = null;
		for (int i = 0; i < shapes.length; i++) {
			if (i >= Waveform.NUM_SHAPES) throw new Exception(String.format("You defined %d shapes, but only %d shapes are supported.", shapes.length, Waveform.NUM_SHAPES));
			shapes[i] = shapes[i].trim();
			if (shapes[i].isEmpty()) continue;
			String[] parts = shapes[i].trim().split(",");
			if (parts.length > 4) throw new Exception(String.format("Your shape specification '%s' contains more than 4 fields.", shapes[i]));
			shape[i] = Waveform.Shape.fromString(parts[0].trim());
			if (shape[i] == null) throw new Exception(String.format("Unknown shape '%s'.", parts[0]));
			if (parts.length >= 2) {
				try {
					int v = Integer.parseInt(parts[1].trim());
					if (v < -100 || v > 100) throw new Exception(String.format("Not a valid weight value: '%s'.", parts[1]));
					weight[i] = (float)v / 100.0f;
				} catch (Exception e) {
					throw new Exception(String.format("Not a valid weight value: '%s'.", parts[1]), e);
				}
			}
			if (parts.length >= 3) {
				try {
					int v = Integer.parseInt(parts[2].trim());
					if (v < -100 || v > 100) throw new Exception(String.format("Not a valid speed value: '%s'.", parts[2]));
					speed[i] = (float)v / 100.0f;
				} catch (Exception e) {
					throw new Exception(String.format("Not a valid speed value: '%s'.", parts[2]), e);
				}
			}
			if (parts.length >= 4) {
				try {
					int v = Integer.parseInt(parts[3].trim());
					if (v < -100 || v > 100) throw new Exception(String.format("Not a valid phase value: '%s'.", parts[3]));
					phase[i] = (float)v / 100.0f;
				} catch (Exception e) {
					throw new Exception(String.format("Not a valid phase value: '%s'.", parts[3]), e);
				}
			}
		}
	}

	public Shape getShape(int i) {
		return shape[i];
	}

	public void setShape(int i, Shape value) {
		shape[i] = value;
		dirty = true;
		spec = null;
	}

	public float getWeight(int i) {
		return weight[i];
	}

	public void setWeight(int i, float value) {
		weight[i] = value;
		dirty = true;
		spec = null;
	}

	public float getSpeed(int i) {
		return speed[i];
	}

	public void setSpeed(int i, float value) {
		speed[i] = value;
		dirty = true;
		spec = null;
	}

	public float getPhase(int i) {
		return phase[i];
	}

	public void setPhase(int i, float value) {
		phase[i] = value;
		dirty = true;
		spec = null;
	}

	public float[] render() {
		if (dirty) {
			Arrays.fill(samples, 0.0f);
			for (int i = 0; i < NUM_SHAPES; i++) {
				if (shape[i] == null) continue;
				float mult = (float)Math.pow(2.0, speed[i]);
				switch (shape[i]) {
					case NONE:
						break;
					case SINE:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							double r = (double)s * mult / NUM_SAMPLES + phase[i];
							samples[s] += Math.sin(r * 2.0 * Math.PI) * weight[i];
						}
						break;
					case SQUARE:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							int r = (int)(mult * s + phase[i] * NUM_SAMPLES) % NUM_SAMPLES;
							samples[s] += (r < NUM_SAMPLES/2 ? weight[i] : -weight[i]);
						}
						break;
					case TRIANGLE:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							float r = (float)s * mult / NUM_SAMPLES + phase[i];
							r = (r < 0.0 ? 1.0f - (Math.abs(r) % 1.0f) : r % 1.0f);
							samples[s] += weight[i] * (r < 0.5f ? 1.0f - Math.abs(4.0f * r - 1.0f) : Math.abs(4.0f * r - 3.0f) - 1.0f);
						}
						break;
					case BIPOLAR:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							int r = (int)(mult * s * 6 / NUM_SAMPLES + phase[i] * 6) % 6;
							samples[s] += (r == 1 ? weight[i] : r == 4 ? -weight[i] : 0.0f);
						}
						break;
					case UNIPOLAR:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							int r = (int)(mult * s * 6 / NUM_SAMPLES + phase[i] * 6) % 6;
							samples[s] += (r == 1 ? weight[i] : 0.0f);
						}
						break;
					case SAWTOOTH:
						for (int s = 0; s < NUM_SAMPLES; s++) {
							float r = (float)s * mult / NUM_SAMPLES + phase[i];
							r = (r < 0.0 ? 1.0f - (Math.abs(r) % 1.0f) : r % 1.0f);
							samples[s] += weight[i] * (r < 0.5f ? 2.0f * r : 2.0f * r - 2.0f);
						}
						break;
				}
			}
			double rms = 0.0f;
			for (int s = 0; s < NUM_SAMPLES; s++) {
				rms += samples[s] * samples[s];
			}
			rms = Math.sqrt(rms / NUM_SAMPLES);
			System.out.println(rms);
			dirty = false;
		}
		return samples;
	}

	/**
	* Interpolation constructor.
	*/

	public Waveform(float s1, Waveform w1, float s2, Waveform w2) {
		// TODO
		this(w1);
		//interpolate(s1, w1, s2, w2);
	}

	public void interpolate(float s1, Waveform w1, float s2, Waveform w2) {
		// TODO
		for (int i = 0; i < NUM_SHAPES; i++) {
			shape[i] = w1.shape[i];
			weight[i] = w1.weight[i];
			speed[i] = w1.speed[i];
			phase[i] = w1.phase[i];
		}
		dirty = true;
		spec = null;
		//for (int i = 0; i < params.length; i++) {
		//	setParam(i, s1 * w1.getParam(i) + s2 * w2.getParam(i));
		//}
		//normalize();
	}

	public Element toXML(Document doc) {
		Element xml = doc.createElement("waveform");
		xml.setAttribute("spec", toString());
		return xml;
	}

	@Override
	public String toString() {
		if (spec == null) {
			spec = "";
			for (int i = 0; i < Waveform.NUM_SHAPES; i++) {
				if (shape[i] == null || shape[i].equals(Waveform.Shape.NONE)) continue;
				if (!spec.isEmpty()) spec += ";";
				spec += shape[i].name().toLowerCase();
				boolean hasPhase = (phase[i] != 0.0f);
				boolean hasSpeed = (speed[i] != 0.0f || hasPhase);
				boolean hasWeight = (weight[i] != 1.0f || hasSpeed);
				if (hasWeight) spec += String.format(",%.0f", weight[i] * 100.0f);
				if (hasSpeed) spec += String.format(",%.0f", speed[i] * 100.0f);
				if (hasPhase) spec += String.format(",%.0f", phase[i] * 100.0f);
			}
		}
		return spec;
	}
}



