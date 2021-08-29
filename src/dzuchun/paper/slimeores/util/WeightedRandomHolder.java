package dzuchun.paper.slimeores.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import dzuchun.paper.slimeores.SlimeOres;

public class WeightedRandomHolder<T> extends LinkedHashMap<T, Integer> {
	@SuppressWarnings("unused")
	private static final Logger LOG = SlimeOres.getInstance().LOG;
	private static final long serialVersionUID = 1L;

	public WeightedRandomHolder(Map<T, Integer> map) {
		super(map);
	}

	public WeightedRandomHolder() {
		super(0);
	}

	private Map<Double, T> compiledProbabillity;

	public void compile() {
		compiledProbabillity = new LinkedHashMap<>(0);
		double totalWeight = this.values().stream().reduce(0, Integer::sum);
		int currentWeight = 0;
		for (Map.Entry<T, Integer> entry : this.entrySet()) {
			currentWeight += entry.getValue();
			compiledProbabillity.put(currentWeight / totalWeight, entry.getKey());
		}
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
