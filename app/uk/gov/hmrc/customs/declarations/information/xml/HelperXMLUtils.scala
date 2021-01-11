/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customs.declarations.information.xml
import scala.annotation.tailrec
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, NamespaceBinding, Node}

object HelperXMLUtils {

  def createPrefixTransformer(inputPrefixToOutputPrefixMap: Map[String, String],
                              nsb: NamespaceBinding): RuleTransformer = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem => {
          val targetPrefix =
            inputPrefixToOutputPrefixMap.getOrElse(e.prefix, e.prefix)
          e.copy(prefix = targetPrefix, scope = nsb)
        }
        case n => n
      }
    })
  }

  def extractNamespaceBindings(node: Node): Seq[NamespaceBinding] = {
    node match {
      case element: Elem => walkNamespaceBindingHierarchy(element.scope)
      case _             => Seq()
    }
  }

  @tailrec
  def walkNamespaceBindingHierarchy(ns: NamespaceBinding,
                                    childBindings: Seq[NamespaceBinding] =
                                     Seq.empty): Seq[NamespaceBinding] = {
    if (ns.uri == null) {
      childBindings
    } else {
      walkNamespaceBindingHierarchy(ns.parent, childBindings :+ ns)
    }
  }

  @tailrec
  def extractNamespacesFromAllElements(node: Node, remainingNodes: Seq[Node] = Seq.empty, acc: Seq[NamespaceBinding] = Seq.empty): Seq[NamespaceBinding] = {
    val stillToProcess = node.child ++ remainingNodes

    if (stillToProcess.size < 1) {
      acc ++ extractNamespaceBindings(node)
    } else {
      extractNamespacesFromAllElements(stillToProcess.head, stillToProcess.tail, acc ++ extractNamespaceBindings(node))
    }
  }
}
