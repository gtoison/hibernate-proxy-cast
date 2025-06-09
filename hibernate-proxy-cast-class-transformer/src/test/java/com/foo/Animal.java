package com.foo;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;

@Entity
@Inheritance
public class Animal implements Mortal {
}
