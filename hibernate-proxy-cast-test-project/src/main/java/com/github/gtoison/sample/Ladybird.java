/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;

/**
 * @author gtoison
 */
@Entity
public class Ladybird extends WingedInsect {

	public Ladybird() {
	}

	public Ladybird(Integer id, String name) {
		super(id, name);
	}

	@Override
	public String flapWings() {
		return "I'm a ladybird: " + super.flapWings();
	}
}
