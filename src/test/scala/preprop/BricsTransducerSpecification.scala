package strsolver.preprop

import scala.collection.mutable.HashMap

import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop._

import dk.brics.automaton.{Automaton => BAutomaton, State, Transition}

object BricsTransducerSpecification
	extends Properties("BricsTransducer"){

  def seq(s : String) = s.map(_.toInt)

  property("Simple Pre +3") = {
    // Transducer q0 -- [a-c], +3 --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("", Plus(3), ""))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[d]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('d', 'd', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val pre = btran.preImage(baut)

    pre(List('a')) && !pre(List('c')) && !pre(List('d'))
  }

  property("Simple Pre With Pre") = {
    // Transducer q0 -- [a-c], ("zz", +0, "") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), ""))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[z]-> q2 -[a-z]-> q3 -[b]-> q4
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    val q3 = new IDState(3)
    val q4 = new IDState(4)
    q4.setAccept(true)
    q1.addTransition(new Transition('z', 'z', q2))
    q2.addTransition(new Transition('a', 'z', q3))
    q3.addTransition(new Transition('b', 'b', q4))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val pre = btran.preImage(baut)

    pre(List('b')) && !pre(List('a')) && !pre(List('d'))
  }

  property("Simple Pre With Pre and Post") = {
    // Transducer q0 -- [a-c], ("zz", +0, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), "adb"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[z]-> q2 -[a-z]-> q3 -[b]-> q4 -- [a] --> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    val q3 = new IDState(3)
    val q4 = new IDState(4)
    q4.setAccept(true)
    q1.addTransition(new Transition('z', 'z', q2))
    q2.addTransition(new Transition('a', 'z', q3))
    q3.addTransition(new Transition('b', 'b', q4))
    q4.addTransition(new Transition('a', 'a', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val pre = btran.preImage(baut)

    pre(List('b')) && !pre(List('a')) && !pre(List('d'))
  }

  property("Simple Pre With Pre and Post Should fail") = {
    // Transducer q0 -- [a-c], ("zz", +0, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), "ad"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[z]-> q2 -[a-z]-> q3 -[b]-> q4 -- [a] --> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    val q3 = new IDState(3)
    val q4 = new IDState(4)
    q4.setAccept(true)
    q1.addTransition(new Transition('z', 'z', q2))
    q2.addTransition(new Transition('a', 'z', q3))
    q3.addTransition(new Transition('b', 'b', q4))
    q4.addTransition(new Transition('a', 'a', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val pre = btran.preImage(baut)

    !pre(List('b')) && !pre(List('a')) && !pre(List('d'))
  }

  property("Simple Pre With Pre and Post and Delete") = {
    // Transducer q0 -- [a-c], ("zz", delete, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Delete, "badb"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[z]-> q2 -[a-z]-> q3 -[b]-> q4 -- [a] --> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    val q3 = new IDState(3)
    val q4 = new IDState(4)
    q4.setAccept(true)
    q1.addTransition(new Transition('z', 'z', q2))
    q2.addTransition(new Transition('a', 'z', q3))
    q3.addTransition(new Transition('b', 'b', q4))
    q4.addTransition(new Transition('a', 'a', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val pre = btran.preImage(baut)

    pre(List('b')) && pre(List('a')) && !pre(List('d', 'e'))
  }

  property("Simple Post +3") = {
    // Transducer q0 -- [a-c], +3 --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("", Plus(3), ""))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[c]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('c', 'c', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val post = btran.postImage(baut)

    post(List('f')) && !post(List('c')) && !post(List('z'))
  }

  property("Simple Post With Pre") = {
    // Transducer q0 -- [a-c], ("zz", +0, "") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), ""))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[c]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('c', 'c', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val post = btran.postImage(baut)

    !post(seq("zza")) && post(seq("zzc")) && !post(List('a'))
  }

  property("Simple Post With Pre and Post") = {
    // Transducer q0 -- [a-c], ("zz", +0, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), "adb"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[c]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('c', 'c', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val post = btran.postImage(baut)

    post(seq("zzcadb")) && !post(seq("aadb")) && !post(seq("zzaadb"))
  }

  property("Simple Post With Pre and Post Should fail") = {
    // Transducer q0 -- [a-c], ("zz", +0, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Plus(0), "ad"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[d]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('d', 'd', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val post = btran.postImage(baut)

    !post(List('b')) && !post(List('a')) && !post(List('d'))
  }

  property("Simple Post With Pre and Post and Delete") = {
    // Transducer q0 -- [a-c], ("zz", delete, "adb") --> qf
    val q0 = new IDState(0)
    val qf = new IDState(1)
    qf.setAccept(true)

    val operations = new HashMap[(State, Transition), OutputOp]
    val t = new Transition('a', 'c', qf)
    operations += ((q0, t) -> new OutputOp("zz", Delete, "adb"))
    q0.addTransition(t)

    val tran = new BAutomaton
    tran.setInitialState(q0)
    val btran = new BricsTransducer(tran, operations.toMap)

    // Automaton q1 -[c]-> q2
    val q1 = new IDState(1)
    val q2 = new IDState(2)
    q2.setAccept(true)
    q1.addTransition(new Transition('c', 'c', q2))
    val aut = new BAutomaton
    aut.setInitialState(q1)
    val baut = new BricsAutomaton(aut)

    val post = btran.postImage(baut)

    post(seq("zzadb")) && !post(seq("aadb")) && !post(seq("d"))
  }
}
