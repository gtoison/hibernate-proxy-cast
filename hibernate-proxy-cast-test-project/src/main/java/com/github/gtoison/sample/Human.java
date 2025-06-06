/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 * 
 */
@Entity
public class Human extends Animal {
	private Animal pet;

	public Human() {
	}

	public Human(Integer id, String name, Animal pet) {
		super(id, name);
		this.pet = pet;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public Animal getPet() {
		return pet;
	}

	public void setPet(Animal pet) {
		this.pet = pet;
	}

	@Transient
	public Mollusc getPetMollusc() {
		return (Mollusc) getPet();
	}

	@Override
	@Transient
	public String getNickname() {
		return getName() + " and my pet is " + pet.getName();
	}
}
