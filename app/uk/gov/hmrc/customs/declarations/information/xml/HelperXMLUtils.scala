/*
 * Copyright 2020 HM Revenue & Customs
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

  /*
  import scala.xml.{Elem, NamespaceBinding, Node, TopScope}
  def changeNS(el: Elem,
               oldURI: String, newURI: String,
               oldPrefix: String, newPrefix: String): Elem = {
    def replace(what: String, before: String, after: String): String =
      if (what == before) after else what

    def fixScope(ns: NamespaceBinding): NamespaceBinding =
      if(ns == TopScope)
        TopScope
      else new NamespaceBinding(replace(ns.prefix, oldPrefix, newPrefix),
        replace(ns.uri, oldURI, newURI),
        fixScope(ns.parent))

    def fixSeq(ns: Seq[Node]): Seq[Node] = for(node <- ns) yield node match {
      case Elem(prefix, label, attribs, scope, children @ _*) =>
        Elem(replace(prefix, oldPrefix, newPrefix),
          label,
          attribs,
          fixScope(scope),
          fixSeq(children) : _*)
      case other => other
    }

    fixSeq(el.theSeq)(0).asInstanceOf[Elem]
  }*/

  /*node.head match {
       case Elem(prefix, label, attribs, scope, children @ _*) =>
         println(s"prefix: $prefix")
         println(s"label: $label")
         println(s"attribs: $attribs")
         println(s"scope: $scope")
         println(s"children#: ${children.size}")

         println(s"scope: ${scope.prefix}")
         println(s"scope: ${scope.uri}")

         println(s"scope: ${scope.parent.parent.parent.parent.parent.prefix}")
         println(s"scope: ${scope.parent.parent.parent.parent.parent.uri}")
     }*/

  def createPrefixTransformer(targetPrefix: String): RuleTransformer = {
    new RuleTransformer( new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem => e.copy(prefix = targetPrefix)
        case n => n
      }
    })
  }

  def extractNamespaceBindings(element: Elem): Seq[NamespaceBinding] = {
    walkNamespaceBindingHierarchy(element.scope)
  }

  @tailrec
  private def walkNamespaceBindingHierarchy(ns: NamespaceBinding, childBindings: Seq[NamespaceBinding] = Seq.empty): Seq[NamespaceBinding] = {
    if (ns.uri == null){
      childBindings
    }
    else {
      walkNamespaceBindingHierarchy(ns.parent, childBindings :+ ns)
    }
  }
}
