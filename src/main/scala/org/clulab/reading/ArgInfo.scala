package org.clulab.reading



case class ArgInfo(label: String, constraints: Seq[Constraint], optional: Boolean, path: String = "") {
  def constraintString: String = {
    s"[${constraints.filter(!_.empty).map(c => c.str).mkString(" & ")}]"
  }
  def withLocalContraint(constraint: Constraint): ArgInfo = {
    ArgInfo(label, constraints ++ Seq(constraint), optional, path)
  }
}

