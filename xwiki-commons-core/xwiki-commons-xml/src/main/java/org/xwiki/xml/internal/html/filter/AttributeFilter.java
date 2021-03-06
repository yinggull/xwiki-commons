/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.xml.internal.html.filter;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Filters attributes that are used by many different elements. Writing transformations for these attributes is tedious
 * because tag transformation don't support matching all tags (e.g. using a wildcard character like '*') and so we need
 * to duplicate the transformation for each element.
 * <p>
 * The following changes are made by this filter:
 * <ul>
 * <li>{@code align="value"} is replaced with {@code style="text-align:value"}</li>
 * <li>{@code valign="value"} is replaced with {@code style="vertical-align:value"}</li>
 * </ul>
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("attribute")
@Singleton
public class AttributeFilter extends AbstractHTMLFilter
{
    /**
     * The map between HTML attribute names and the corresponding CSS property name.
     */
    private static final Map<String, String> ATTRIBUTE_TO_CSS_PROPERTY = new HashMap<String, String>();

    /**
     * The logger.
     */
    @Inject
    private Logger logger;

    {
        ATTRIBUTE_TO_CSS_PROPERTY.put("align", "text-align");
        ATTRIBUTE_TO_CSS_PROPERTY.put("valign", "vertical-align");
        ATTRIBUTE_TO_CSS_PROPERTY.put("bgcolor", "background-color");
    }

    @Override
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        StringBuilder xpathExpression = new StringBuilder();
        for (String attributeName : ATTRIBUTE_TO_CSS_PROPERTY.keySet()) {
            if (xpathExpression.length() > 0) {
                xpathExpression.append('|');
            }
            xpathExpression.append("//@").append(attributeName);
        }

        NodeList attributes = null;
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            attributes = (NodeList) xpath.evaluate(xpathExpression.toString(), document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            // Shouldn't happen.
            logger.error("Failed to apply the HTML attribute cleaning filter.", e);
            return;
        }

        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            Element element = attribute.getOwnerElement();
            StringBuilder style = new StringBuilder(element.getAttribute(ATTRIBUTE_STYLE).trim());
            if (style.length() > 0 && style.charAt(style.length() - 1) != ';') {
                style.append(';');
            }
            style.append(ATTRIBUTE_TO_CSS_PROPERTY.get(attribute.getName()));
            style.append(':').append(attribute.getValue());
            element.setAttribute(ATTRIBUTE_STYLE, style.toString());
            element.removeAttributeNode(attribute);
        }
    }
}
