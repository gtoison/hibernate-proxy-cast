/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;

/**
 * @author gtoison
 */
@Entity
public class Insect extends Animal {

	public Insect() {
	}

	public Insect(Integer id, String name) {
		super(id, name);
	}
}
