/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>.
 */

package hm.binkley.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.String.format;
import static org.junit.Assert.fail;

/**
 * {@code MetaInfBindingsAnnotationProcessorTest} tests {@link MetaInfBindingsAnnotationProcessor}.
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Needs documentation.
 */
public class MetaInfBindingsAnnotationProcessorTest {
    @Test
    public void shouldFindFredInMetaInfBindings()
            throws IOException {
        final BufferedReader next = new BufferedReader(new InputStreamReader(
                getClass().getResource("/META-INF/bindings/" + Bob.class.getName()).openStream()));
        try {
            String line;
            while (null != (line = next.readLine()))
                if (Fred.class.getName().equals(line))
                    return;
        } finally {
            next.close();
        }

        fail(format("Did not find %s in /META-INF/bindings/%s", Fred.class.getName(),
                Bob.class.getName()));
    }

    public interface Bob {}

    @MetaInfBindings(Bob.class)
    public static final class Fred
            implements Bob {}
}
