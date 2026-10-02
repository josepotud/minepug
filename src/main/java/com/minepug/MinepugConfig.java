package com.minepug;

/**
 * Ajustes de probabilidad del mod.
 * <p>
 * Probabilidad de que un cachorro sea carlino en función de las generaciones de perros
 * domesticados que tengan los padres detrás:
 * <pre>
 * probabilidad = min(BASE + POR_GENERACIÓN * generaciónPadres, MÁXIMO)
 * </pre>
 * Un lobo recién domesticado tiene generación 0, así que dos lobos salvajes
 * domesticados crían con un 10 % de probabilidad; si los padres ya nacieron de
 * una cría (generación 1) es un 20 %, y así sucesivamente hasta el 90 %.
 */
public final class MinepugConfig {
	public static final double BASE_CHANCE = 0.10D;
	public static final double CHANCE_PER_GENERATION = 0.10D;
	public static final double MAX_CHANCE = 0.90D;

	private MinepugConfig() {
	}

	public static double pugChance(int parentsGeneration) {
		return Math.min(BASE_CHANCE + CHANCE_PER_GENERATION * Math.max(0, parentsGeneration), MAX_CHANCE);
	}
}
