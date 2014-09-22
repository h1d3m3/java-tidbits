package stream;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamClosureTest {

	 private Integer b = 2;

	List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

	public StreamClosureTest() {
		while (true) {
			System.out.println(calculate(list.stream(), new Int(3)).collect(Collectors.toList()));
//			b = new Integer(b++); // Bad news, the closure is using the old ref.
			 b++; // In place variable change. Closure is OK with this.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}

	private Stream<Integer> calculate(Stream<Integer> stream, final Int a) {
		return stream.map(new Function<Integer, Integer>() {

			public Integer apply(Integer t) {
				 return t * a.value + getB();
			}

		});
	}

	static private class Int {
		public int value;

		public Int(int value) {
			this.value = value;
		}
	}

	private int getB() {
		return this.b;
	}

	public static void main(String[] args) {
		new StreamClosureTest();
	}
}
