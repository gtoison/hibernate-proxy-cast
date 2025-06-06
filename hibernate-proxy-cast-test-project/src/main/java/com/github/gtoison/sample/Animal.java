/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * 
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Animal implements Nicknamed {
	private Integer id;
	private String name;

	public Animal() {
	}

	public Animal(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
