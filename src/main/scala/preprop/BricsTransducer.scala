/*
 * This file is part of Sloth, an SMT solver for strings.
 * Copyright (C) 2018  Matthew Hague, Philipp Ruemmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package strsolver.preprop

import scala.collection.mutable.{HashSet => MHashSet,
                                 HashMap => MHashMap,
                                 Stack => MStack}

import dk.brics.automaton.{Automaton => BAutomaton,
                           State => BState,
                           Transition => BTransition}

import scala.collection.JavaConversions.iterableAsScalaIterable

object BricsTransducer {
  def apply() : BricsTransducer =
    new BricsTransducer(new BAutomaton, Map[(BState, BTransition), OutputOp]())

  def getBuilder : BricsTransducerBuilder =
    new BricsTransducerBuilder
}

/**
 * Implementation of transducers as automata with input and output
 * states.  That is, from an input state, all transitions read a
 * character from input.  From an output state, all transitions produce
 * a character of output
 */
class BricsTransducer(override val underlying : BAutomaton,
                      val operations : Map[(BState, BTransition), OutputOp])
  extends BricsAutomaton(underlying) with AtomicStateTransducer {

  /**
   * Iterate over outgoing transition with operation
   */
  def outgoingTransitionsOps : Iterator[(State, TLabel, OutputOp)] =
    for (t <- from.getTransitions.iterator)
    yield (t.getDest, (t.getMin, t.getMax), operations((from, t)))

  def preImage(aut : AtomicStateAutomaton) : AtomicStateAutomaton = {
    val preBuilder = aut.getBuilder

    // map states of pre-image aut to state of transducer and state of
    // aut
    val sMap = new MHashMap[aut.State, (State, aut.State)]
    val sMapRev = new MHashMap[(State, aut.State), aut.State]

    val initState = underlying.getInitialState
    val initAutState = aut.initialState
    val newInitState = preBuilder.getNewState
    preBuilder setInitialState newInitState

    sMap += (newInitState -> ((initState, initAutState)))
    sMapRev += (initState, initAutState) -> newInitState

    // transducer state, automaton state
    def getState(ts : State, as : aut.State) = {
      sMapRev.getOrElse((ts, as), {
        val ps = preBuilder.getNewState
        sMapRev += ((ts, as) -> ps)
        sMap += (ps -> (ts, as))
        ps
      })
    }

    // when working through a transition ..
    abstract class Mode
    // .. either doing pre part (u remains to do)
    case class Pre(u : Seq[Char]) extends Mode
    // .. applying operation
    case object Op extends Mode
    // .. or working through post part, once done any new transition
    // added to pre-image aut should have label lbl
    case class Post(u : Seq[Char], lbl : aut.TLabel) extends Mode

    // (ps, ts, t, as, m)
    // state of pre aut to add new transitions from
    // current state of transducer reached
    // transition being processed
    // current state of target aut reached
    // mode as above
    val worklist = new MStack[(aut.State, State, BTransition, aut.State, Mode)]
    val seenlist = new MHashSet[(aut.State, State, BTransition, aut.State, Mode)]

    def addWork(ps : aut.State ,
                ts : State,
                t : BTransition,
                as : aut.State,
                m : Mode) {
      if (!seenlist.contains((ps, ts, t, as, m))) {
        seenlist += ((ps, ts, t, as, m))
        worklist.push((ps, ts, t, as, m))
      }
    }

    def reachStates(ts : State, as : aut.State) {
      val ps = getState(ts, as)
      if (isAccept(ts) && aut.isAccept(as))
        preBuilder.setAccept(ps, true)

      for (t <- ts.getTransitions) {
        val tOp = operations(ts, t)
        if (tOp.preW.isEmpty)
          addWork(ps, ts, t, as, Op)
        else
          addWork(ps, ts, t, as, Pre(tOp.preW))
      }
    }

    reachStates(initialState, aut.initialState)

    while (!worklist.isEmpty) {
      // pre aut state, transducer state, automaton state
      val (ps, ts, t, as, m) = worklist.pop()

      m match {
        case Pre(u) if u.isEmpty => {
          // should never happen
        }
        case Pre(u) if !u.isEmpty => {
          val a = u.head
          val rest = u.tail
          for ((asNext, albl) <- aut.outgoingTransitions(as)) {
            if (aut.LabelOps.labelContains(a, albl)) {
              if (!rest.isEmpty) {
                addWork(ps, ts, t, asNext, Pre(rest))
              } else {
                addWork(ps, ts, t, asNext, Op)
              }
            }
          }
        }
        case Op => {
          val tOp = operations(ts, t)
          tOp.op match {
            case Delete => {
              val lbl = aut.LabelOps.interval(t.getMin, t.getMax)
              addWork(ps, ts, t, as, Post(tOp.postW, lbl))
            }
            case Plus(n) => {
              for ((asNext, albl) <- aut.outgoingTransitions(as)) {
                val shftLbl = aut.LabelOps.shift(albl, n)
                if (aut.LabelOps.isNonEmptyLabel(shftLbl)) {
                  val tlbl = aut.LabelOps.interval(t.getMin, t.getMax)
                  for (preLbl <- aut.LabelOps.intersectLabels(shftLbl, tlbl)) {
                    addWork(ps, ts, t, asNext, Post(tOp.postW, preLbl))
                  }
                }
              }
            }
          }
        }
        case Post(v, lbl) if !v.isEmpty => {
          val a = v.head
          val rest = v.tail
          for ((asNext, albl) <- aut.outgoingTransitions(as)) {
            if (aut.LabelOps.labelContains(a, albl))
              addWork(ps, ts, t, asNext, Post(rest, lbl))
          }
        }
        case Post(v, lbl) if v.isEmpty => {
          val psNext = getState(t.getDest, as)
          val tsNext = t.getDest

          preBuilder.addTransition(ps, lbl, psNext)

          reachStates(tsNext, as)
        }
      }
    }

    preBuilder.getAutomaton
  }

  override def toString = {
    super.toString + '\n' + operations.mkString("\n")
  }
}


class BricsTransducerBuilder
    extends AtomicStateTransducerBuilder[BricsAutomaton#State,
                                         BricsAutomaton#TLabel] {
  val LabelOps : TLabelOps[BricsAutomaton#TLabel] = BricsTLabelOps

  val aut = {
    val baut = new BAutomaton
    baut.setDeterministic(false)
    new BricsAutomaton(baut)
  }
  val operations = new MHashMap[(BState, BTransition), OutputOp]

  lazy val initialState : BricsAutomaton#State = aut.initialState

  def getNewState : BricsAutomaton#State = new BState

  def setAccept(s : BricsAutomaton#State, isAccept : Boolean) = s.setAccept(isAccept)

  def addTransition(s1 : BricsAutomaton#State,
                    lbl : BricsAutomaton#TLabel,
                    op : OutputOp,
                    s2 : BricsAutomaton#State) = {
    if (aut.LabelOps.isNonEmptyLabel(lbl)) {
      val t = new BTransition(lbl._1, lbl._2, s2)
      s1.addTransition(t)
      operations += ((s1, t) -> op)
    }
  }

  def getTransducer = {
    // do not restore invariant since will break operations map
    new BricsTransducer(aut.underlying, operations.toMap)
  }
}

