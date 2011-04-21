/*
 * Copyright (c) 2011 Matthias Matouesk <matou@taunusstein.net>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import java.io.*;
import java.util.*;

public class WhAsm {

    private static BufferedReader stdin = 
        new BufferedReader(new InputStreamReader(System.in));

    final private static String comment = "//";

    private static HashMap<String, Command> commands;

    public static void main(String[] args) throws IOException {
        init();
        String line, command, param;
        while((line=stdin.readLine())!=null) {
            // ignore empty lines
            if (line.trim().equals("")) continue;

            // get rid of comments
            line = line.split(comment)[0].trim();
            if (line.equals("")) continue;

            command = line.split("\\s+")[0];
            param = "";
            if (line.split("\\s+").length >= 2)
                for (int i=1; i<line.split("\\s+").length; i++)
                    param += line.split("\\s+")[i] + " ";

            commands.get(command).output(param);
        }
    }

    private static void init() {
        commands = new HashMap<String, Command>();

        // stack manipulation
        String cmd = "push";
        commands.put(cmd, new Command(cmd, "  ", true));
        cmd = "duplicate";
        commands.put(cmd, new Command(cmd, " \n ", false));
        cmd = "copyn";
        commands.put(cmd, new Command(cmd, " \t ", true));
        cmd = "swap";
        commands.put(cmd, new Command(cmd, " \n\t", false));
        cmd = "discard";
        commands.put(cmd, new Command(cmd, " \n\n", false));
        cmd = "sliden";
        commands.put(cmd, new Command(cmd, " \t\n", true));

        // arithmetic
        cmd = "add";
        commands.put(cmd, new Command(cmd, "\t   "));
        cmd = "sub";
        commands.put(cmd, new Command(cmd, "\t  \t"));
        cmd = "mult";
        commands.put(cmd, new Command(cmd, "\t  \n"));
        cmd = "div";
        commands.put(cmd, new Command(cmd, "\t \t "));
        cmd = "mod";
        commands.put(cmd, new Command(cmd, "\t \t\t"));

        // heap access
        cmd = "store";
        commands.put(cmd, new Command(cmd, "\t\t "));
        cmd = "retrieve";
        commands.put(cmd, new Command(cmd, "\t\t\t"));

        // flow control
        cmd = "mark"; // see pseudo commands
        commands.put(cmd, new Command(cmd, "\n  ", true));
        cmd = "call";
        commands.put(cmd, new Command(cmd, "\n \t", true));
        cmd = "jump";
        commands.put(cmd, new Command(cmd, "\n \n", true));
        cmd = "branchz";
        commands.put(cmd, new Command(cmd, "\n\t ", true));
        cmd = "branchltz";
        commands.put(cmd, new Command(cmd, "\n\t\t", true));
        cmd = "return";
        commands.put(cmd, new Command(cmd, "\n\t\n"));
        cmd = "end";
        commands.put(cmd, new Command(cmd, "\n\n\n"));

        // I/O
        cmd = "printchar";
        commands.put(cmd, new Command(cmd, "\t\n  "));
        cmd = "printnum";
        commands.put(cmd, new Command(cmd, "\t\n \t"));
        cmd = "readchar";
        commands.put(cmd, new Command(cmd, "\t\n\t "));
        cmd = "readnum";
        commands.put(cmd, new Command(cmd, "\t\n\t\t"));

        // pseudo commands
        // pushes a character to the stack
        cmd = "pushchar";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("push")
                    .output(Integer.toString((int)param.toCharArray()[0]));
            }
        });

        // prints a string to the screen
        cmd = "print";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                char[] p = param.toCharArray();
                for (char c : p) {
                    commands.get("pushchar").output(""+c);
                    commands.get("printchar").output("");
                }
            }
        });

        // prints a string followed by a newline
        cmd = "println";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("print").output(param);
                commands.get("push").output("0x0A");
                commands.get("printchar").output("");
            }
        });

        // marks the programm with a label 
        cmd = "marks";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("mark").output(labelToBinary(param));
            }
        });


        // jumps to the given label (label is a string)
        cmd = "jumps";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("jump").output(labelToBinary(param));
            }
        });

        cmd = "calls";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("call").output(labelToBinary(param));
            }
        });

        cmd = "branchzs";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("branchz").output(labelToBinary(param));
            }
        });

        cmd = "branchltzs";
        commands.put(cmd, new Command(cmd, "") {
            void output(String param) {
                commands.get("branchltz").output(labelToBinary(param));
            }
        });

    }

    private static String labelToBinary(String label) {
        String mark = "0b";
        for (char c : label.toCharArray()) {
            mark += Integer.toString((int)c,2);
        }
        return mark;
    }

}

class Command {

    private String mnemonic;
    private String cmd;
    private boolean param;

    Command(String mnemonic, String command) {
        this(mnemonic, command, false);
    }

    Command(String mnemonic, String command, boolean param) {
        this.mnemonic = mnemonic;
        this.cmd = command;
        this.param = param;
    }

    void output(String param) {
        System.out.print(this.cmd);
       
        param = param.trim();
        if (this.param) {
            // binary
            if (param.startsWith("0b"))
                System.out.println( 
                        param
                        .replace("0b","")
                        .replace('0',' ')
                        .replace('1','\t'));
            // hex
            else if (param.startsWith("0x"))
                output0(Integer.parseInt(param.replace("0x",""), 16));
            // decimal
            else 
                output0(Integer.parseInt(param));
        }
    }

    private void output0(int p) {
        if (p<0) System.out.print('\t');
        else System.out.print(' ');
        System.out.println(
                Integer.toString(p,2)
                .replace("-","")
                .replace('0',' ')
                .replace('1','\t'));

    }

    public boolean equals(Object o) {
        return this.mnemonic.equals(o);
    }

    public int hashCode() {
        return this.mnemonic.hashCode();
    }

}

