package dzuchun.paper.slimeores.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class WeightedRandomHolder<T> extends LinkedHashMap<T, Integer> {
	private static final long serialVersionUID = 1L;

	public WeightedRandomHolder(Map<T, Integer> map) {
		super(map);
	}

	public WeightedRandomHolder() {
		super(0);
	}

	private Map<Double, T> compiledProbabillity;

	public WeightedRandomHolder<T> compile() {
		compiledProbabillity = new LinkedHashMap<>(0);
		double totalWeight = this.values().stream().reduce(0, Integer::sum);
		int currentWeight = 0;
		for (Map.Entry<T, Integer> entry : this.entrySet()) {
			currentWeight += entry.getValue();
			compiledProbabillity.put(currentWeight / totalWeight, entry.getKey());
		}
		return this;
	}

	public T getForRandom(Random random) {
		double rand = random.nextDouble();
		for (Map.Entry<Double, T> entry : compiledProbabillity.entrySet()) {
			if (entry.getKey() > rand) {
				return entry.getValue();
			}
		}
		return null;
	}

}
