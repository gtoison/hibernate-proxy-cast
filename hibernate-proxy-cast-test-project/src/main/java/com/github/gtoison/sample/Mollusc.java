/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;

/**
 * 
 */
@Entity
public class Mollusc extends Animal {
	boolean hasShell;

	public Mollusc() {
	}

	public Mollusc(Integer id, String name, boolean hasShell) {
		super(id, name);
		this.hasShell = hasShell;
	}
}
