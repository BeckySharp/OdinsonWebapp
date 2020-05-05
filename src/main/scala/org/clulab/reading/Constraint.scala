package org.clulab.reading

trait Constraint {
  def str: String
  def empty: Boolean = false
}
case class TokenConstraint(field: String, value: String, negated: Boolean = false) extends Constraint {
  def str: String = s"$field=$value"
}

// Used for wildcards: []
case class EmptyConstraint() extends Constraint {
  def str: String = ""
  override def empty: Boolean = true
}

// Was being used for things like: (word=cat | entity=ORG)
case class OrConstraint(c1: Constraint, c2: Constraint) extends Constraint {
  def str: String = s"(${c1.str} | ${c2.str})"
}