/**
 * 
 */
package com.github.gtoison.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class AnimalChecker {
	public static final Logger LOGGER = LoggerFactory.getLogger(AnimalChecker.class);

	public boolean isMollusc(Animal a) {
		return a instanceof Mollusc;
	}

	public Mollusc getHumanPetAsMollusc(Animal a) {
		if (a instanceof Human human && human.getPet() instanceof Mollusc mollusc) {
			return mollusc;
		}

		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <A extends Animal> A covarPetCast(Human human, Class<A> type) {
		return (A) human.getPet();
	}
	
	public Mollusc getHumanPetAsMolluscWithCovarPetCast(Human human) {
		Mollusc mollusc = covarPetCast(human, Mollusc.class);
		
		return mollusc;
	}

	public String wingedFlapWings(Animal a) {
		return ((Winged) a).flapWings();
	}

	public String wingedInsectFlapWings(Animal a) {
		return ((WingedInsect) a).flapWings();
	}

	public String nickname(Object o) {
		if (o instanceof Nicknamed n) {
			return n.getNickname();
		}

		return null;
	}

	public String name(Object o) {
		if (o instanceof Named n) {
			return n.getName();
		}

		return null;
	}

	public String nickNamedName(Object o) {
		if (o instanceof Nicknamed n) {
			return n.getName();
		}

		return null;
	}
}
