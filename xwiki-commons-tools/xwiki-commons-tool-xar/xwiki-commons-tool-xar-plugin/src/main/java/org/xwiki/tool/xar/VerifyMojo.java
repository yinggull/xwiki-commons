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
package org.xwiki.tool.xar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Perform various verifications of the XAR files in this project. Namely:
 * <ul>
 *   <li>ensure that pages all have a parent (except for Main.WebHome)</li>
 *   <li>ensure that the author/contentAuthor/creator is {@code xwiki:XWiki.Admin}</li>
 *   <li>ensure that the version is {@code 1.1}</li>
 * </ul>
 *
 * @version $Id$
 * @goal verify
 * @phase verify
 * @requiresProject
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class VerifyMojo extends AbstractVerifyMojo
{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Checking validity of XAR XML files...");

        boolean hasErrors = false;
        for (File file : getXARXMLFiles()) {
            String parentName = file.getParentFile().getName();
            XWikiDocument xdoc = getDocFromXML(file);
            List<String> errors = new ArrayList<String>();

            // Verification 1: Verify authors
            verifyAuthor(errors, xdoc.getAuthor(), String.format("Author must be [%s] but was [%s]",
                AUTHOR, xdoc.getAuthor()));
            verifyAuthor(errors, xdoc.getContentAuthor(),
                String.format("Content Author must be [%s] but was [%s]",
                    AUTHOR, xdoc.getContentAuthor()));
            verifyAuthor(errors, xdoc.getCreator(), String.format("Creator must be [%s] but was [%s]",
                AUTHOR, xdoc.getCreator()));
            // Verification 2: Check for orphans, except for Main.WebHome since it's the topmost document
            if (StringUtils.isEmpty(xdoc.getParent())
                && !(xdoc.getSpace().equals("Main") && xdoc.getName().equals("WebHome")))
            {
                errors.add("Parent must not be empty");
            }
            // Verification 3: Check for version
            if (!xdoc.getVersion().equals(VERSION)) {
                errors.add(String.format("Version must be [%s] but was [%s]", VERSION, xdoc.getVersion()));
            }

            if (errors.isEmpty()) {
                getLog().info(String.format("  Verifying [%s/%s]... ok", parentName, file.getName()));
            } else {
                getLog().info(String.format("  Verifying [%s/%s]... errors", parentName, file.getName()));
                for (String error : errors) {
                    getLog().info(String.format("  - %s", error));
                }
                hasErrors = true;
            }
        }

        if (hasErrors) {
            throw new MojoFailureException("There are errors in the XAR XML files!");
        }
    }

    private void verifyAuthor(List<String> errors, String author, String message)
    {
        if (!author.equals(AUTHOR)) {
            errors.add(message);
        }
    }
}