/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli2.option;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.cli2.DisplaySetting;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.HelpLine;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.WriteableCommandLine;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.WriteableCommandLineImpl;

/**
 * @author Rob Oxspring
 */
public class GroupTest
    extends GroupTestCase {
    public static final Command COMMAND_START =
        new Command("start", "Starts the server", null, false, null, null, 0);
    public static final Command COMMAND_STOP =
        new Command("stop", "Stops the server", null, false, null, null, 0);
    public static final Command COMMAND_RESTART =
        new Command("restart", "Stops and starts the server", null, false, null, null, 0);
    public static final Command COMMAND_GRACEFUL =
        new Command("graceful", "Restarts the server without interruption", null, false, null,
                    null, 0);

    public static Group buildApacheCommandGroup() {
        final List options = new ArrayList();
        options.add(COMMAND_GRACEFUL);
        options.add(COMMAND_RESTART);
        options.add(COMMAND_START);
        options.add(COMMAND_STOP);

        return new GroupImpl(options, "httpd-cmds", "The command to pass to the server", 1, 1, true);
    }

    public static Group buildApachectlGroup() {
        final List options = new ArrayList();
        options.add(DefaultOptionTest.buildHelpOption());
        options.add(ParentTest.buildKParent());

        return new GroupImpl(options, "apachectl", "Controls the apache http deamon", 0,
                             Integer.MAX_VALUE, true);
    }

    public static Group buildAntGroup() {
        final List options = new ArrayList();
        options.add(DefaultOptionTest.buildHelpOption());
        options.add(ArgumentTest.buildTargetsArgument());

        return new GroupImpl(options, "ant", "The options for ant", 0, Integer.MAX_VALUE, true);
    }

    private static Group buildRequiredTestGroup(final boolean required,
            final int minimum)
    {
        final Group group = new GroupImpl(new ArrayList(), "test", null,
                minimum, Integer.MAX_VALUE, required);
        final List options = new ArrayList(1);
        options.add(group);
        new GroupImpl(options, "parent", null, 0, Integer.MAX_VALUE, false);
        return group;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.GroupTestCase#testProcessAnonymousArguments()
     */
    public void testProcessAnonymousArguments()
        throws OptionException {
        final Group option = buildAntGroup();
        final List args = list("compile,test", "dist");
        final ListIterator iterator = args.listIterator();
        final WriteableCommandLine commandLine = commandLine(option, args);
        option.process(commandLine, iterator);

        assertFalse(iterator.hasNext());
        assertTrue(commandLine.hasOption("target"));
        assertListContentsEqual(commandLine.getValues("target"), args);
        assertListContentsEqual(list("compile", "test", "dist"), args);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.GroupTestCase#testProcessOptions()
     */
    public void testProcessOptions()
        throws OptionException {
        final Group option = buildApachectlGroup();
        final List args = list("-?", "-k");
        final ListIterator iterator = args.listIterator();
        final WriteableCommandLine commandLine = commandLine(option, args);
        option.process(commandLine, iterator);

        assertFalse(iterator.hasNext());
        assertTrue(commandLine.hasOption("--help"));
        assertTrue(commandLine.hasOption("-k"));
        assertFalse(commandLine.hasOption("start"));
        assertListContentsEqual(list("--help", "-k"), args);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testCanProcess()
     */
    public void testCanProcess() {
        final Group option = buildApacheCommandGroup();
        assertTrue(option.canProcess(new WriteableCommandLineImpl(option, null), "start"));
    }

    public void testCanProcess_BadMatch() {
        final Group option = buildApacheCommandGroup();
        assertFalse(option.canProcess(new WriteableCommandLineImpl(option, null), "begin"));
    }

    public void testCanProcess_NullMatch() {
        final Group option = buildApacheCommandGroup();
        assertFalse(option.canProcess(new WriteableCommandLineImpl(option, null), (String) null));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testPrefixes()
     */
    public void testPrefixes() {
        final Group option = buildApachectlGroup();
        assertContentsEqual(list("-", "--"), option.getPrefixes());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testProcess()
     */
    public void testProcess()
        throws OptionException {
        final Group option = buildAntGroup();
        final List args = list("--help", "compile,test", "dist");
        final ListIterator iterator = args.listIterator();
        final WriteableCommandLine commandLine = commandLine(option, args);
        option.process(commandLine, iterator);

        assertFalse(iterator.hasNext());
        assertTrue(commandLine.hasOption("-?"));
        assertListContentsEqual(list("compile", "test", "dist"), commandLine.getValues("target"));
    }

    public void testProcess_Nested()
        throws OptionException {
        final Group option = buildApachectlGroup();
        final List args = list("-h", "-k", "graceful");
        final ListIterator iterator = args.listIterator();
        final WriteableCommandLine commandLine = commandLine(option, args);
        option.process(commandLine, iterator);

        assertFalse(iterator.hasNext());
        assertTrue(commandLine.hasOption("-?"));
        assertTrue(commandLine.hasOption("-k"));
        assertTrue(commandLine.hasOption("graceful"));
        assertFalse(commandLine.hasOption("stop"));
        assertTrue(commandLine.getValues("start").isEmpty());
        assertListContentsEqual(list("--help", "-k", "graceful"), args);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testTriggers()
     */
    public void testTriggers() {
        final Group option = buildApachectlGroup();
        assertContentsEqual(list("--help", "-?", "-h", "-k"), option.getTriggers());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testValidate()
     */
    public void testValidate()
        throws OptionException {
        final Group option = buildApacheCommandGroup();
        final WriteableCommandLine commandLine = commandLine(option, list());

        commandLine.addOption(COMMAND_RESTART);

        option.validate(commandLine);
    }

    public void testValidate_UnexpectedOption() {
        final Group option = buildApacheCommandGroup();
        final WriteableCommandLine commandLine = commandLine(option, list());

        commandLine.addOption(COMMAND_RESTART);
        commandLine.addOption(COMMAND_GRACEFUL);

        try {
            option.validate(commandLine);
            fail("Too many options");
        } catch (OptionException uoe) {
            assertEquals(option, uoe.getOption());
        }
    }

    public void testValidate_MissingOption() {
        final Group option = buildApacheCommandGroup();
        final WriteableCommandLine commandLine = commandLine(option, list());

        try {
            option.validate(commandLine);
            fail("Missing an option");
        } catch (OptionException moe) {
            assertEquals(option, moe.getOption());
        }
    }

    public void testValidate_RequiredChild()
        throws OptionException {
        final Option required =
            new DefaultOptionBuilder().withLongName("required").withRequired(true).create();
        final Option optional =
            new DefaultOptionBuilder().withLongName("optional").withRequired(false).create();
        final Group group =
            new GroupBuilder().withOption(required).withOption(optional).withMinimum(1).create();

        WriteableCommandLine commandLine;

        commandLine = commandLine(group, list());

        try {
            group.validate(commandLine);
            fail("Missing option 'required'");
        } catch (OptionException moe) {
            assertEquals(required, moe.getOption());
        }

        commandLine = commandLine(group, list());
        commandLine.addOption(optional);

        try {
            group.validate(commandLine);
            fail("Missing option 'required'");
        } catch (OptionException moe) {
            assertEquals(required, moe.getOption());
        }

        commandLine = commandLine(group, list());
        commandLine.addOption(required);
        group.validate(commandLine);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testAppendUsage()
     */
    public void testAppendUsage() {
        final Option option = buildApacheCommandGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);

        //settings.remove(DisplaySetting.DISPLAY_ARGUMENT_NUMBERED);
        option.appendUsage(buffer, settings, null);

        assertEquals("httpd-cmds (graceful|restart|start|stop)", buffer.toString());
    }

    public void testAppendUsage_NoOptional() {
        final Option option = buildApacheCommandGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_OPTIONAL);
        option.appendUsage(buffer, settings, null);

        assertEquals("httpd-cmds (graceful|restart|start|stop)", buffer.toString());
    }

    public void testAppendUsage_NoExpand() {
        final Option option = buildApacheCommandGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_EXPANDED);
        option.appendUsage(buffer, settings, null);

        assertEquals("httpd-cmds", buffer.toString());
    }

    public void testAppendUsage_NoExpandOrName() {
        final Option option = buildApacheCommandGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_EXPANDED);
        settings.remove(DisplaySetting.DISPLAY_GROUP_NAME);
        option.appendUsage(buffer, settings, null);

        assertEquals("httpd-cmds", buffer.toString());
    }

    public void testAppendUsage_NoName() {
        final Option option = buildApacheCommandGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_NAME);
        option.appendUsage(buffer, settings, null);

        assertEquals("graceful|restart|start|stop", buffer.toString());
    }

    public void testAppendUsage_WithArgs() {
        final Option option = buildAntGroup();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_OUTER);
        option.appendUsage(buffer, settings, null);

        assertEquals("[ant (--help (-?,-h)) [<target1> [<target2> ...]]]", buffer.toString());
    }

    public void testAppendUsage_OptionalChildGroup() {
        final Option option = buildRequiredTestGroup(false, 2).getParent();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        option.appendUsage(buffer, settings, null);

        assertEquals("[parent ([test ()])]", buffer.toString());
    }

    public void testAppendUsage_OptionalChildGroupNoSetting() {
        final Option option = buildRequiredTestGroup(false, 2).getParent();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_OPTIONAL_CHILD_GROUP);
        option.appendUsage(buffer, settings, null);

        assertEquals("[parent (test ())]", buffer.toString());
    }

    public void testAppendUsage_RequiredChildGroup() {
        final Option option = buildRequiredTestGroup(true, 2).getParent();
        final StringBuffer buffer = new StringBuffer();
        final Set settings = new HashSet(DisplaySetting.ALL);
        option.appendUsage(buffer, settings, null);

        assertEquals("[parent (test ())]", buffer.toString());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testGetPreferredName()
     */
    public void testGetPreferredName() {
        final Option option = buildAntGroup();
        assertEquals("ant", option.getPreferredName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testGetDescription()
     */
    public void testGetDescription() {
        final Option option = buildApachectlGroup();
        assertEquals("Controls the apache http deamon", option.getDescription());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testHelpLines()
     */
    public void testHelpLines() {
        final Option option = buildApacheCommandGroup();
        final List lines = option.helpLines(0, DisplaySetting.ALL, null);
        final Iterator i = lines.iterator();

        final HelpLine line1 = (HelpLine) i.next();
        assertEquals(0, line1.getIndent());
        assertEquals(option, line1.getOption());

        final HelpLine line2 = (HelpLine) i.next();
        assertEquals(1, line2.getIndent());
        assertEquals(COMMAND_GRACEFUL, line2.getOption());

        final HelpLine line3 = (HelpLine) i.next();
        assertEquals(1, line3.getIndent());
        assertEquals(COMMAND_RESTART, line3.getOption());

        final HelpLine line4 = (HelpLine) i.next();
        assertEquals(1, line4.getIndent());
        assertEquals(COMMAND_START, line4.getOption());

        final HelpLine line5 = (HelpLine) i.next();
        assertEquals(1, line5.getIndent());
        assertEquals(COMMAND_STOP, line5.getOption());

        assertFalse(i.hasNext());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testHelpLines()
     */
    public void testHelpLines_NoExpanded() {
        final Option option = buildApacheCommandGroup();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_EXPANDED);

        final List lines = option.helpLines(0, settings, null);
        final Iterator i = lines.iterator();

        final HelpLine line1 = (HelpLine) i.next();
        assertEquals(0, line1.getIndent());
        assertEquals(option, line1.getOption());

        assertFalse(i.hasNext());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.cli2.OptionTestCase#testHelpLines()
     */
    public void testHelpLines_NoName() {
        final Option option = buildApacheCommandGroup();
        final Set settings = new HashSet(DisplaySetting.ALL);
        settings.remove(DisplaySetting.DISPLAY_GROUP_NAME);

        final List lines = option.helpLines(0, settings, null);
        final Iterator i = lines.iterator();

        final HelpLine line2 = (HelpLine) i.next();
        assertEquals(1, line2.getIndent());
        assertEquals(COMMAND_GRACEFUL, line2.getOption());

        final HelpLine line3 = (HelpLine) i.next();
        assertEquals(1, line3.getIndent());
        assertEquals(COMMAND_RESTART, line3.getOption());

        final HelpLine line4 = (HelpLine) i.next();
        assertEquals(1, line4.getIndent());
        assertEquals(COMMAND_START, line4.getOption());

        final HelpLine line5 = (HelpLine) i.next();
        assertEquals(1, line5.getIndent());
        assertEquals(COMMAND_STOP, line5.getOption());

        assertFalse(i.hasNext());
    }

    /**
     * Tests isRequired() for a child group if neither the required flag nor a
     * minimum constraint is set.
     */
    public void testIsRequired_ChildNoFlagNoMinimum()
    {
        final Group group = buildRequiredTestGroup(false, 0);
        assertFalse("Group is required", group.isRequired());
    }

    /**
     * Tests isRequired() for a child group that has a minimum constraint, but
     * the required flag is not set.
     */
    public void testIsRequired_ChildNoFlagMinimum()
    {
        final Group group = buildRequiredTestGroup(false, 10);
        assertFalse("Group is required", group.isRequired());
    }

    /**
     * Tests isRequired() for a child group that has the required flag set, but
     * no minimum constraint. In this constellation the group is de facto not
     * required.
     */
    public void testIsRequired_ChildFlagNoMinimum()
    {
        final Group group = buildRequiredTestGroup(true, 0);
        assertFalse("Group is required", group.isRequired());
    }

    /**
     * Tests isRequired() for a child group that has both the required flag and
     * a minimum constraint set. This is indeed a required group.
     */
    public void testIsRequired_ChildFlagMinimum()
    {
        final Group group = buildRequiredTestGroup(true, 10);
        assertTrue("Group is not required", group.isRequired());
    }

    /**
     * Tests isRequired() for the root group when no minimum constraint is set.
     */
    public void testIsRequired_ParentNoMinimum()
    {
        final Group parent = (Group) buildRequiredTestGroup(false, 0)
                .getParent();
        assertFalse("Group is required", parent.isRequired());
    }

    /**
     * Tests isRequired() for the root group with a minimum constraint.
     */
    public void testIsRequired_ParentMiminum()
    {
        final Group parent = new GroupImpl(new ArrayList(), "test", null, 10,
                Integer.MAX_VALUE, false);
        assertTrue("Group not required", parent.isRequired());
    }
}
