/**
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

package org.apache.commons.cli;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Abstract test case testing common parser features.
 *
 * @author Emmanuel Bourg
 * @version $Revision$, $Date$
 */
public abstract class ParserTestCase extends TestCase
{
    protected CommandLineParser parser;

    protected Options options;

    public void setUp()
    {
        options = new Options()
            .addOption("a", "enable-a", false, "turn [a] on or off")
            .addOption("b", "bfile", true, "set the value of [b]")
            .addOption("c", "copt", false, "turn [c] on or off");
    }

    public void testSimpleShort() throws Exception
    {
        String[] args = new String[] { "-a",
                                       "-b", "toast",
                                       "foo", "bar" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("toast"));
        assertTrue("Confirm size of extra args", cl.getArgList().size() == 2);
    }

    public void testSimpleLong() throws Exception
    {
        String[] args = new String[] { "--enable-a",
                                       "--bfile", "toast",
                                       "foo", "bar" };

        CommandLine cl = parser.parse(options, args);

        assertTrue( "Confirm -a is set", cl.hasOption("a") );
        assertTrue( "Confirm -b is set", cl.hasOption("b") );
        assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
        assertTrue( "Confirm arg of --bfile", cl.getOptionValue( "bfile" ).equals( "toast" ) );
        assertTrue( "Confirm size of extra args", cl.getArgList().size() == 2);
    }

    public void testMultiple() throws Exception
    {
        String[] args = new String[] { "-c",
                                       "foobar",
                                       "-b", "toast" };

        CommandLine cl = parser.parse(options, args, true);
        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertTrue("Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);

        cl = parser.parse(options, cl.getArgs());

        assertTrue("Confirm -c is not set", !cl.hasOption("c"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("toast"));
        assertTrue("Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
        assertTrue("Confirm  value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("foobar"));
    }

    public void testMultipleWithLong() throws Exception
    {
        String[] args = new String[] { "--copt",
                                       "foobar",
                                       "--bfile", "toast" };

        CommandLine cl = parser.parse(options, args, true);
        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertTrue("Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);

        cl = parser.parse(options, cl.getArgs());

        assertTrue("Confirm -c is not set", !cl.hasOption("c"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("toast"));
        assertTrue("Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
        assertTrue("Confirm  value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("foobar"));
    }

    public void testUnrecognizedOption() throws Exception
    {
        String[] args = new String[] { "-a", "-d", "-b", "toast", "foo", "bar" };

        try
        {
            parser.parse(options, args);
            fail("UnrecognizedOptionException wasn't thrown");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("-d", e.getOption());
        }
    }

    public void testMissingArg() throws Exception
    {
        String[] args = new String[] { "-b" };

        boolean caught = false;

        try
        {
            parser.parse(options, args);
        }
        catch (MissingArgumentException e)
        {
            caught = true;
            assertEquals("option missing an argument", "b", e.getOption().getOpt());
        }

        assertTrue( "Confirm MissingArgumentException caught", caught );
    }

    public void testDoubleDash1() throws Exception
    {
        String[] args = new String[] { "--copt",
                                       "--",
                                       "-b", "toast" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertTrue("Confirm -b is not set", !cl.hasOption("b"));
        assertTrue("Confirm 2 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 2);
    }

    public void testSingleDash() throws Exception
    {
        String[] args = new String[] { "--copt",
                                       "-b", "-",
                                       "-a",
                                       "-" };

        CommandLine cl = parser.parse(options, args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("-"));
        assertTrue("Confirm 1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
        assertTrue("Confirm value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("-"));
    }

    public void testStopAtUnexpectedArg() throws Exception
    {
        String[] args = new String[] { "-c",
                                       "foober",
                                       "-b",
                                       "toast" };

        CommandLine cl = parser.parse(options, args, true);
        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertTrue("Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);
    }

   public void testStopAtExpectedArg() throws Exception
    {
        String[] args = new String[]{"-b", "foo"};

        CommandLine cl = parser.parse(options, args, true);

        assertTrue("Confirm -b is set", cl.hasOption('b'));
        assertEquals("Confirm -b is set", "foo", cl.getOptionValue('b'));
        assertTrue("Confirm no extra args: " + cl.getArgList().size(), cl.getArgList().size() == 0);
    }

    public void testStopAtNonOptionShort() throws Exception
    {
        String[] args = new String[]{"-z",
                                     "-a",
                                     "-btoast"};

        CommandLine cl = parser.parse(options, args, true);
        assertFalse("Confirm -a is not set", cl.hasOption("a"));
        assertTrue("Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);
    }

    public void testStopAtNonOptionLong() throws Exception
    {
        String[] args = new String[]{"--zop==1",
                                     "-abtoast",
                                     "--b=bar"};

        CommandLine cl = parser.parse(options, args, true);

        assertFalse("Confirm -a is not set", cl.hasOption("a"));
        assertFalse("Confirm -b is not set", cl.hasOption("b"));
        assertTrue("Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);
    }

    public void testNegativeArgument() throws Exception
    {
        String[] args = new String[] { "-b", "-1"} ;

        CommandLine cl = parser.parse(options, args);
        assertEquals("-1", cl.getOptionValue("b"));
    }

    public void testArgumentStartingWithHyphen() throws Exception
    {
        String[] args = new String[]{"-b", "-foo"};

        CommandLine cl = parser.parse(options, args);
        assertEquals("-foo", cl.getOptionValue("b"));
    }

    public void testShortWithEqual() throws Exception
    {
        String[] args = new String[] { "-f=bar" };

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("foo").hasArg().create('f'));

        CommandLine cl = parser.parse(options, args);

        assertEquals("bar", cl.getOptionValue("foo"));
    }

    public void testShortWithoutEqual() throws Exception
    {
        String[] args = new String[] { "-fbar" };

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("foo").hasArg().create('f'));

        CommandLine cl = parser.parse(options, args);

        assertEquals("bar", cl.getOptionValue("foo"));
    }

    public void testLongWithEqualDoubleDash() throws Exception
    {
        String[] args = new String[] { "--foo=bar" };

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("foo").hasArg().create('f'));

        CommandLine cl = parser.parse(options, args);

        assertEquals("bar", cl.getOptionValue("foo"));
    }

    public void testLongWithEqualSingleDash() throws Exception
    {
        String[] args = new String[] { "-foo=bar" };

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("foo").hasArg().create('f'));

        CommandLine cl = parser.parse(options, args);

        assertEquals("bar", cl.getOptionValue("foo"));
    }

    public void testPropertiesOption1() throws Exception
    {
        String[] args = new String[] { "-Jsource=1.5", "-J", "target", "1.5", "foo" };

        Options options = new Options();
        options.addOption(OptionBuilder.withValueSeparator().hasArgs(2).create('J'));

        CommandLine cl = parser.parse(options, args);

        List values = Arrays.asList(cl.getOptionValues("J"));
        assertNotNull("null values", values);
        assertEquals("number of values", 4, values.size());
        assertEquals("value 1", "source", values.get(0));
        assertEquals("value 2", "1.5", values.get(1));
        assertEquals("value 3", "target", values.get(2));
        assertEquals("value 4", "1.5", values.get(3));
        
        List argsleft = cl.getArgList();
        assertEquals("Should be 1 arg left", 1, argsleft.size());
        assertEquals("Expecting foo", "foo", argsleft.get(0));
    }

    public void testPropertiesOption2() throws Exception
    {
        String[] args = new String[] { "-Dparam1", "-Dparam2=value2", "-D"};

        Options options = new Options();
        options.addOption(OptionBuilder.withValueSeparator().hasOptionalArgs(2).create('D'));

        CommandLine cl = parser.parse(options, args);
        
        Properties props = cl.getOptionProperties("D");
        assertNotNull("null properties", props);
        assertEquals("number of properties in " + props, 2, props.size());
        assertEquals("property 1", "true", props.getProperty("param1"));
        assertEquals("property 2", "value2", props.getProperty("param2"));
        
        List argsleft = cl.getArgList();
        assertEquals("Should be no arg left", 0, argsleft.size());
    }

    public void testUnambiguousPartialLongOption1() throws Exception
    {
        String[] args = new String[] { "--ver" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("help").create());
        
        CommandLine cl = parser.parse(options, args);
        
        assertTrue("Confirm --version is set", cl.hasOption("version"));
    }

    public void testUnambiguousPartialLongOption2() throws Exception
    {
        String[] args = new String[] { "-ver" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("help").create());
        
        CommandLine cl = parser.parse(options, args);
        
        assertTrue("Confirm --version is set", cl.hasOption("version"));
    }

    public void testUnambiguousPartialLongOption3() throws Exception
    {
        String[] args = new String[] { "--ver=1" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("verbose").hasOptionalArg().create());
        options.addOption(OptionBuilder.withLongOpt("help").create());
        
        CommandLine cl = parser.parse(options, args);
        
        assertTrue("Confirm --verbose is set", cl.hasOption("verbose"));
        assertEquals("1", cl.getOptionValue("verbose"));
    }

    public void testUnambiguousPartialLongOption4() throws Exception
    {
        String[] args = new String[] { "-ver=1" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("verbose").hasOptionalArg().create());
        options.addOption(OptionBuilder.withLongOpt("help").create());
        
        CommandLine cl = parser.parse(options, args);
        
        assertTrue("Confirm --verbose is set", cl.hasOption("verbose"));
        assertEquals("1", cl.getOptionValue("verbose"));
    }
    
    public void testAmbiguousPartialLongOption1() throws Exception
    {
        String[] args = new String[] { "--ver" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("verbose").create());
        
        boolean caught = false;
        
        try 
        {
            parser.parse(options, args);
        }
        catch (AmbiguousOptionException e) 
        {
            caught = true;
            assertEquals("Partial option", "--ver", e.getOption());
            assertNotNull("Matching options null", e.getMatchingOptions());
            assertEquals("Matching options size", 2, e.getMatchingOptions().size());
        }
        
        assertTrue( "Confirm MissingArgumentException caught", caught );
    }
    
    public void testAmbiguousPartialLongOption2() throws Exception
    {
        String[] args = new String[] { "-ver" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("verbose").create());
        
        boolean caught = false;
        
        try 
        {
            parser.parse(options, args);
        }
        catch (AmbiguousOptionException e) 
        {
            caught = true;
            assertEquals("Partial option", "-ver", e.getOption());
            assertNotNull("Matching options null", e.getMatchingOptions());
            assertEquals("Matching options size", 2, e.getMatchingOptions().size());
        }
        
        assertTrue( "Confirm MissingArgumentException caught", caught );
    }

    public void testAmbiguousPartialLongOption3() throws Exception
    {
        String[] args = new String[] { "--ver=1" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("verbose").hasOptionalArg().create());
        
        boolean caught = false;
        
        try 
        {
            parser.parse(options, args);
        }
        catch (AmbiguousOptionException e) 
        {
            caught = true;
            assertEquals("Partial option", "--ver", e.getOption());
            assertNotNull("Matching options null", e.getMatchingOptions());
            assertEquals("Matching options size", 2, e.getMatchingOptions().size());
        }
        
        assertTrue( "Confirm MissingArgumentException caught", caught );
    }

    public void testAmbiguousPartialLongOption4() throws Exception
    {
        String[] args = new String[] { "-ver=1" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.withLongOpt("verbose").hasOptionalArg().create());
        
        boolean caught = false;
        
        try 
        {
            parser.parse(options, args);
        }
        catch (AmbiguousOptionException e) 
        {
            caught = true;
            assertEquals("Partial option", "-ver", e.getOption());
            assertNotNull("Matching options null", e.getMatchingOptions());
            assertEquals("Matching options size", 2, e.getMatchingOptions().size());
        }
        
        assertTrue( "Confirm MissingArgumentException caught", caught );
    }
    
    public void testPartialLongOptionSingleDash() throws Exception
    {
        String[] args = new String[] { "-ver" };
        
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("version").create());
        options.addOption(OptionBuilder.hasArg().create('v'));
        
        CommandLine cl = parser.parse(options, args);
        
        assertTrue("Confirm --version is set", cl.hasOption("version"));
        assertTrue("Confirm -v is not set", !cl.hasOption("v"));
    }

    public void testWithRequiredOption() throws Exception
    {
        String[] args = new String[] { "-b", "file" };
        
        Options options = new Options();
        options.addOption("a", "enable-a", false, null);
        options.addOption(OptionBuilder.withLongOpt("bfile").hasArg().isRequired().create('b'));

        CommandLine cl = parser.parse(options,args);

        assertTrue("Confirm -a is NOT set", !cl.hasOption("a"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("file"));
        assertTrue("Confirm NO of extra args", cl.getArgList().size() == 0);
    }

    public void testOptionAndRequiredOption() throws Exception
    {
        String[] args = new String[] { "-a", "-b", "file" };
        
        Options options = new Options();
        options.addOption("a", "enable-a", false, null);
        options.addOption(OptionBuilder.withLongOpt("bfile").hasArg().isRequired().create('b'));

        CommandLine cl = parser.parse(options,args);

        assertTrue("Confirm -a is set", cl.hasOption("a"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("file"));
        assertTrue("Confirm NO of extra args", cl.getArgList().size() == 0);
    }

    public void testMissingRequiredOption()
    {
        String[] args = new String[] { "-a" };
        
        Options options = new Options();
        options.addOption("a", "enable-a", false, null);
        options.addOption(OptionBuilder.withLongOpt("bfile").hasArg().isRequired().create('b'));

        try
        {
            parser.parse(options,args);
            fail("exception should have been thrown");
        }
        catch (MissingOptionException e)
        {
            assertEquals( "Incorrect exception message", "Missing required option: b", e.getMessage() );
            assertTrue(e.getMissingOptions().contains("b"));
        }
        catch (ParseException e)
        {
            fail("expected to catch MissingOptionException");
        }
    }

    public void testMissingRequiredOptions()
    {
        String[] args = new String[] { "-a" };

        Options options = new Options();
        options.addOption("a", "enable-a", false, null);
        options.addOption(OptionBuilder.withLongOpt("bfile").hasArg().isRequired().create('b'));
        options.addOption(OptionBuilder.withLongOpt("cfile").hasArg().isRequired().create('c'));

        try
        {
            parser.parse(options,args);
            fail("exception should have been thrown");
        }
        catch (MissingOptionException e)
        {
            assertEquals("Incorrect exception message", "Missing required options: b, c", e.getMessage());
            assertTrue(e.getMissingOptions().contains("b"));
            assertTrue(e.getMissingOptions().contains("c"));
        }
        catch (ParseException e)
        {
            fail("expected to catch MissingOptionException");
        }
    }
    
    public void testMissingRequiredGroup() throws Exception
    {
        OptionGroup group = new OptionGroup();
        group.addOption(OptionBuilder.create("a"));
        group.addOption(OptionBuilder.create("b"));
        group.setRequired(true);

        Options options = new Options();
        options.addOptionGroup(group);
        options.addOption(OptionBuilder.isRequired().create("c"));

        try
        {
            parser.parse(options, new String[] { "-c" });
            fail("MissingOptionException not thrown");
        }
        catch (MissingOptionException e)
        {
            assertEquals(1, e.getMissingOptions().size());
            assertTrue(e.getMissingOptions().get(0) instanceof OptionGroup);
        }
        catch (ParseException e)
        {
            fail("Expected to catch MissingOptionException");
        }
    }
    
    public void testOptionGroup() throws Exception
    {
        OptionGroup group = new OptionGroup();
        group.addOption(OptionBuilder.create("a"));
        group.addOption(OptionBuilder.create("b"));
        
        Options options = new Options();
        options.addOptionGroup(group);
        
        parser.parse(options, new String[] { "-b" });
        
        assertEquals("selected option", "b", group.getSelected());
    }

    public void testOptionGroupLong() throws Exception
    {
        OptionGroup group = new OptionGroup();
        group.addOption(OptionBuilder.withLongOpt("foo").create());
        group.addOption(OptionBuilder.withLongOpt("bar").create());
        
        Options options = new Options();
        options.addOptionGroup(group);
        
        CommandLine cl = parser.parse(options, new String[] { "--bar" });
        
        assertTrue(cl.hasOption("bar"));
        assertEquals("selected option", "bar", group.getSelected());
    }
    
    public void testReuseOptionsTwice() throws Exception
    {
        Options opts = new Options();
		opts.addOption(OptionBuilder.isRequired().create('v'));

        // first parsing
        parser.parse(opts, new String[] { "-v" });

        try
        {
            // second parsing, with the same Options instance and an invalid command line
            parser.parse(opts, new String[0]);
            fail("MissingOptionException not thrown");
        }
        catch (MissingOptionException e)
        {
            // expected
        }
    }

    public void testBursting() throws Exception
    {
        String[] args = new String[] { "-acbtoast", "foo", "bar" };

        CommandLine cl = parser.parse(options, args);

        assertTrue( "Confirm -a is set", cl.hasOption("a") );
        assertTrue( "Confirm -b is set", cl.hasOption("b") );
        assertTrue( "Confirm -c is set", cl.hasOption("c") );
        assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
        assertTrue( "Confirm size of extra args", cl.getArgList().size() == 2);
    }

    public void testUnrecognizedOptionWithBursting() throws Exception
    {
        String[] args = new String[] { "-adbtoast", "foo", "bar" };

        try
        {
            parser.parse(options, args);
            fail("UnrecognizedOptionException wasn't thrown");
        }
        catch (UnrecognizedOptionException e)
        {
            assertEquals("-adbtoast", e.getOption());
        }
    }

    public void testMissingArgWithBursting() throws Exception
    {
        String[] args = new String[] { "-acb" };

        boolean caught = false;

        try
        {
            parser.parse(options, args);
        }
        catch (MissingArgumentException e)
        {
            caught = true;
            assertEquals("option missing an argument", "b", e.getOption().getOpt());
        }

        assertTrue( "Confirm MissingArgumentException caught", caught );
    }

    public void testStopBursting() throws Exception
    {
        String[] args = new String[] { "-azc" };

        CommandLine cl = parser.parse(options, args, true);
        assertTrue( "Confirm -a is set", cl.hasOption("a") );
        assertFalse( "Confirm -c is not set", cl.hasOption("c") );

        assertTrue( "Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
        assertTrue(cl.getArgList().contains("zc"));
    }

    public void testStopBursting2() throws Exception
    {
        String[] args = new String[] { "-c", "foobar", "-btoast" };

        CommandLine cl = parser.parse(options, args, true);
        assertTrue("Confirm -c is set", cl.hasOption("c"));
        assertTrue("Confirm  2 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 2);

        cl = parser.parse(options, cl.getArgs());

        assertTrue("Confirm -c is not set", !cl.hasOption("c"));
        assertTrue("Confirm -b is set", cl.hasOption("b"));
        assertTrue("Confirm arg of -b", cl.getOptionValue("b").equals("toast"));
        assertTrue("Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
        assertTrue("Confirm  value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("foobar"));
    }
}
