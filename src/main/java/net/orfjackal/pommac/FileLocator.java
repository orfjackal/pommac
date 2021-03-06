/*
 * Copyright © 2008-2009  Esko Luontola, www.orfjackal.net
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

package net.orfjackal.pommac;

import net.orfjackal.pommac.util.*;

import java.io.*;
import java.util.regex.Pattern;

/**
 * @author Esko Luontola
 * @since 29.2.2008
 */
public class FileLocator {

    private final File workDir;
    private final WorkDirectoryManager manager;

    public FileLocator() {
        this(new File("").getAbsoluteFile());
    }

    public FileLocator(File workDir) {
        this.workDir = workDir;
        manager = new WorkDirectoryManager(workDir);
    }

    public void dispose() {
        manager.dispose();
    }

    public File findFile(String query) {
        return findFile(workDir, query);
    }

    public File findFile(File dir, String query) {
        String[] parts = query.split("/");
        assert parts.length > 0;
        File current = dir;
        for (String part : parts) {
            boolean expectArchive = false;
            if (part.endsWith("!")) {
                expectArchive = true;
                part = part.substring(0, part.length() - 1);
            }
            current = findFile(current, part, query);
            if (expectArchive) {
                current = unpackToTempDir(current);
            }
        }
        return current;
    }

    private File unpackToTempDir(File archive) {
        File dir = manager.newDirectory();
        ZipUtil.unzip(archive, dir);
        return dir;
    }

    private static File findFile(File dir, String nameQuery, String fullQuery) {
        final Pattern pattern = Pattern.compile(toRegex(nameQuery));
        File[] found = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return pattern.matcher(file.getName()).matches();
            }
        });
        if (found.length == 0) {
            throw new MatchingFileNotFoundException(fullQuery);
        } else if (found.length > 1) {
            throw new UnambiguousQueryException(fullQuery, found);
        }
        return found[0];
    }

    private static String toRegex(String query) {
        StringBuilder regex = new StringBuilder();
        for (char c : query.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append(".");
            } else {
                regex.append(Pattern.quote(Character.toString(c)));
            }
        }
        return regex.toString();
    }
}
