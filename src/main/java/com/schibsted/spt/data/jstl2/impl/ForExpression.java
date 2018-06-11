
package com.schibsted.spt.data.jstl2.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.schibsted.spt.data.jslt.JsltException;

public class ForExpression extends AbstractNode {
  private ExpressionNode valueExpr;
  private LetExpression[] lets;
  private ExpressionNode loopExpr;

  public ForExpression(ExpressionNode valueExpr,
                       LetExpression[] lets,
                       ExpressionNode loopExpr,
                       Location location) {
    super(location);
    this.valueExpr = valueExpr;
    this.lets = lets;
    this.loopExpr = loopExpr;
  }

  public JsonNode apply(Scope scope, JsonNode input) {
    JsonNode array = valueExpr.apply(scope, input);
    if (array.isNull())
      return NullNode.instance;
    else if (array.isObject())
      array = NodeUtils.convertObjectToArray(array);
    else if (!array.isArray())
      throw new JsltException("For loop can't iterate over " + array, location);

    // may be the same, if no lets
    Scope newscope = scope;

    ArrayNode result = NodeUtils.mapper.createArrayNode();
    for (int ix = 0; ix < array.size(); ix++) {
      JsonNode value = array.get(ix);

      // must evaluate lets over again for each value because of context
      if (lets.length > 0)
        newscope = NodeUtils.evalLets(scope, value, lets);

      result.add(loopExpr.apply(newscope, value));
    }
    return result;
  }

  public void computeMatchContexts(DotExpression parent) {
    // if you do matching inside a for the matching is on the current
    // object being traversed in the list. so we forget the parent
    // and start over
    loopExpr.computeMatchContexts(new DotExpression(location));
  }

  public void dump(int level) {
    System.out.println(NodeUtils.indent(level) + "for (");
    valueExpr.dump(level + 1);
    System.out.println(NodeUtils.indent(level) + ")");
    loopExpr.dump(level + 1);
  }

  public String toString() {
    return "[for (" + valueExpr + ") " + loopExpr + "]";
  }
}
