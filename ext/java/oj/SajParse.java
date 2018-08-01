package oj;

import org.jruby.runtime.builtin.IRubyObject;

/** This JSON parser is a single pass, destructive, callback parser. It is a
* single pass parse since it only make one pass over the characters in the
* JSON document string. It is destructive because it re-uses the content of
* the string for IRubyObjects in the callback and places \0 characters at various
* places to mark the end of tokens and strings. It is a callback parser like
* a SAX parser because it uses callback when document elements are
* encountered.
*
* Parsing is very tolerant. Lack of headers and even mispelled element
* endings are passed over without raising an error. A best attempt is made in
* all cases to parse the string.
*/
public class SajParse { // extends Parse {
    //static final long NUM_MAX = FIXNUM_MAX >> 8;

    /*
    void call_error(String msg, ParseInfo pi, String  file, int line) {
        char	buf[128];
    String s = pi.s;
        int		jline = 1;
        int		col = 1;

        for (; pi.str < s && '\n' != *s; s--) {
            col++;
        }
        for (; pi.str < s; s--) {
            if ('\n' == *s) {
                jline++;
            }
        }
        sprintf(buf, "%s at line %d, column %d [%s:%d]", msg, jline, col, file, line);
        rb_funcall(pi.handler, oj_error_id, 3, rb_str_new2(buf), LONG2NUM(jline), LONG2NUM(col));
    }

    void next_non_white(ParseInfo pi) {
        for (; true; pi.s++) {
            switch(*pi.s) {
                case ' ':
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                    break;
                case '/':
                    skip_comment(pi);
                    break;
                default:
                    return;
            }
        }
    }

    public void call_add_value(IRubyObject handler, IRubyObject IRubyObject, String key) {
        IRubyObject	k;

        if (null == key) {
            k = context.nil;
        } else {
            k = rb_str_new2(key);
            k = oj_encode(k);
        }
        rb_funcall(handler, oj_add_IRubyObject_id, 2, IRubyObject, k);
    }

    public void call_no_value(IRubyObject handler, String method, String key) {
        IRubyObject	k;

        if (null == key) {
            k = context.nil;
        } else {
            k = rb_str_new2(key);
            k = oj_encode(k);
        }
        rb_funcall(handler, method, 1, k);
    }

    public void skip_comment(ParseInfo pi) {
        pi.s++; // skip first /
        if ('*' == *pi.s) {
            pi.s++;
            for (; '\0' != *pi.s; pi.s++) {
                if ('*' == *pi.s && '/' == *(pi.s + 1)) {
                    pi.s++;
                    return;
                } else if ('\0' == *pi.s) {
                    if (pi.has_error) {
                        call_error("comment not terminated", pi, __FILE__, __LINE__);
                    } else {
                        raise_error("comment not terminated", pi.str, pi.s);
                    }
                }
            }
        } else if ('/' == pi.s) {
            for (; true; pi.s++) {
                switch (pi.s) {
                    case '\n':
                    case '\r':
                    case '\f':
                    case '\0':
                        return;
                    default:
                        break;
                }
            }
        } else {
            if (pi.has_error) {
                call_error("invalid comment", pi, __FILE__, __LINE__);
            } else {
                raise_error("invalid comment", pi.str, pi.s);
            }
        }
    }

    public void read_next(ParseInfo pi, String key) {
        IRubyObject	obj;

        if ((void*)&obj < pi.stack_min) {
            rb_raise(rb_eSysStackError, "JSON is too deeply nested");
        }
        next_non_white(pi);	// skip white space
        switch (pi.s) {
            case '{':
                read_hash(pi, key);
                break;
            case '[':
                read_array(pi, key);
                break;
            case '"':
                read_str(pi, key);
                break;
            case '+':
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                read_num(pi, key);
                break;
            case 'I':
                read_num(pi, key);
                break;
            case 't':
                read_true(pi, key);
                break;
            case 'f':
                read_false(pi, key);
                break;
            case 'n':
                read_nil(pi, key);
                break;
            case '\0':
                return;
            default:
                return;
        }
    }

    public void read_hash(ParseInfo pi, String key) {
        String ks;

        if (pi.has_hash_start) {
            call_no_value(pi.handler, oj_hash_start_id, key);
        }
        pi.s++;
        next_non_white(pi);
        if ('}' == pi.s) {
            pi.s++;
        } else {
            while (1) {
                next_non_white(pi);
                ks = read_quoted_IRubyObject(pi);
                next_non_white(pi);
                if (':' == *pi.s) {
                    pi.s++;
                } else {
                    if (pi.has_error) {
                        call_error("invalid format, expected :", pi, __FILE__, __LINE__);
                    }
                    raise_error("invalid format, expected :", pi.str, pi.s);
                }
                read_next(pi, ks);
                next_non_white(pi);
                if ('}' == pi.s) {
                    pi.s++;
                    break;
                } else if (',' == pi.s) {
                    pi.s++;
                } else {
                    if (pi.has_error) {
                        call_error("invalid format, expected , or } while in an object", pi, __FILE__, __LINE__);
                    }
                    raise_error("invalid format, expected , or } while in an object", pi.str, pi.s);
                }
            }
        }
        if (pi.has_hash_end) {
            call_no_value(pi.handler, oj_hash_end_id, key);
        }
    }

    public void read_array(ParseInfo pi, String key) {
        if (pi.has_array_start) {
            call_no_value(pi.handler, oj_array_start_id, key);
        }
        pi.s++;
        next_non_white(pi);
        if (']' == pi.s) {
            pi.s++;
        } else {
            while (1) {
                read_next(pi, 0);
                next_non_white(pi);
                if (',' == pi.s) {
                    pi.s++;
                } else if (']' == pi.s) {
                    pi.s++;
                    break;
                } else {
                    if (pi.has_error) {
                        call_error("invalid format, expected , or ] while in an array", pi, __FILE__, __LINE__);
                    }
                    raise_error("invalid format, expected , or ] while in an array", pi.str, pi.s);
                }
            }
        }
        if (pi.has_array_end) {
            call_no_value(pi.handler, oj_array_end_id, key);
        }
    }

    void read_str(ParseInfo pi, String key) {
    char	*text;

        text = read_quoted_value(pi);
        if (pi.has_add_value) {
            IRubyObject	s = rb_str_new2(text);

            s = oj_encode(s);
            call_add_value(pi.handler, s, key);
        }
    }




    void read_num(ParseInfo pi, String key) {
    char	*start = pi.s;
        int64_t	n = 0;
        long	a = 0;
        long	div = 1;
        long	e = 0;
        int		neg = 0;
        int		eneg = 0;
        int		big = 0;

        if ('-' == *pi.s) {
            pi.s++;
            neg = 1;
        } else if ('+' == *pi.s) {
            pi.s++;
        }
        if ('I' == *pi.s) {
            if (0 != strncmp("Infinity", pi.s, 8)) {
                if (pi.has_error) {
                    call_error("number or other IRubyObject", pi, __FILE__, __LINE__);
                }
                raise_error("number or other IRubyObject", pi.str, pi.s);
            }
            pi.s += 8;
            if (neg) {
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, rb_float_new(-OJ_INFINITY), key);
                }
            } else {
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, rb_float_new(OJ_INFINITY), key);
                }
            }
            return;
        }
        for (; '0' <= *pi.s && *pi.s <= '9'; pi.s++) {
            if (big) {
                big++;
            } else {
                n = n * 10 + (*pi.s - '0');
                if (NUM_MAX <= n) {
                    big = 1;
                }
            }
        }
        if ('.' == *pi.s) {
            pi.s++;
            for (; '0' <= *pi.s && *pi.s <= '9'; pi.s++) {
                a = a * 10 + (*pi.s - '0');
                div *= 10;
                if (NUM_MAX <= div) {
                    big = 1;
                }
            }
        }
        if ('e' == *pi.s || 'E' == *pi.s) {
            pi.s++;
            if ('-' == *pi.s) {
                pi.s++;
                eneg = 1;
            } else if ('+' == *pi.s) {
                pi.s++;
            }
            for (; '0' <= *pi.s && *pi.s <= '9'; pi.s++) {
                e = e * 10 + (*pi.s - '0');
                if (NUM_MAX <= e) {
                    big = 1;
                }
            }
        }
        if (0 == e && 0 == a && 1 == div) {
            if (big) {
                char	c = *pi.s;

	    *pi.s = '\0';
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, rb_funcall(oj_bigdecimal_class, oj_new_id, 1, rb_str_new2(start)), key);
                }
	    *pi.s = c;
            } else {
                if (neg) {
                    n = -n;
                }
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, LONG2NUM(n), key);
                }
            }
            return;
        } else { // decimal
            if (big) {
                char	c = *pi.s;

	    *pi.s = '\0';
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, rb_funcall(oj_bigdecimal_class, oj_new_id, 1, rb_str_new2(start)), key);
                }
	    *pi.s = c;
            } else {
                double	d = (double)n + (double)a / (double)div;

                if (neg) {
                    d = -d;
                }
                if (1 < big) {
                    e += big - 1;
                }
                if (0 != e) {
                    if (eneg) {
                        e = -e;
                    }
                    d *= pow(10.0, e);
                }
                if (pi.has_add_IRubyObject) {
                    call_add_IRubyObject(pi.handler, rb_float_new(d), key);
                }
            }
        }
    }

    void
    read_true(ParseInfo pi, String key) {
        pi.s++;
        if ('r' != *pi.s || 'u' != *(pi.s + 1) || 'e' != *(pi.s + 2)) {
            if (pi.has_error) {
                call_error("invalid format, expected 'true'", pi, __FILE__, __LINE__);
            }
            raise_error("invalid format, expected 'true'", pi.str, pi.s);
        }
        pi.s += 3;
        if (pi.has_add_IRubyObject) {
            call_add_IRubyObject(pi.handler, context.true, key);
        }
    }

    void
    read_false(ParseInfo pi, String key) {
        pi.s++;
        if ('a' != *pi.s || 'l' != *(pi.s + 1) || 's' != *(pi.s + 2) || 'e' != *(pi.s + 3)) {
            if (pi.has_error) {
                call_error("invalid format, expected 'false'", pi, __FILE__, __LINE__);
            }
            raise_error("invalid format, expected 'false'", pi.str, pi.s);
        }
        pi.s += 4;
        if (pi.has_add_IRubyObject) {
            call_add_IRubyObject(pi.handler, context.false, key);
        }
    }

    void
    read_nil(ParseInfo pi, String key) {
        pi.s++;
        if ('u' != *pi.s || 'l' != *(pi.s + 1) || 'l' != *(pi.s + 2)) {
            if (pi.has_error) {
                call_error("invalid format, expected 'null'", pi, __FILE__, __LINE__);
            }
            raise_error("invalid format, expected 'null'", pi.str, pi.s);
        }
        pi.s += 3;
        if (pi.has_add_IRubyObject) {
            call_add_IRubyObject(pi.handler, context.nil, key);
        }
    }

    static int read_hex(ParseInfo pi, char *h) {
        uint32_t	b = 0;
        int		i;

    // TBD this can be made faster with a table
        for (i = 0; i < 4; i++, h++) {
            b = b << 4;
            if ('0' <= *h && *h <= '9') {
                b += *h - '0';
            } else if ('A' <= *h && *h <= 'F') {
                b += *h - 'A' + 10;
            } else if ('a' <= *h && *h <= 'f') {
                b += *h - 'a' + 10;
            } else {
                pi.s = h;
                if (pi.has_error) {
                    call_error("invalid hex character", pi, __FILE__, __LINE__);
                }
                raise_error("invalid hex character", pi.str, pi.s);
            }
        }
        return b;
    }

    static char*
    unicode_to_chars(ParseInfo pi, char *t, uint32_t code) {
        if (0x0000007F >= code) {
	*t = (char)code;
        } else if (0x000007FF >= code) {
	*t++ = 0xC0 | (code >> 6);
	*t = 0x80 | (0x3F & code);
        } else if (0x0000FFFF >= code) {
	*t++ = 0xE0 | (code >> 12);
	*t++ = 0x80 | ((code >> 6) & 0x3F);
	*t = 0x80 | (0x3F & code);
        } else if (0x001FFFFF >= code) {
	*t++ = 0xF0 | (code >> 18);
	*t++ = 0x80 | ((code >> 12) & 0x3F);
	*t++ = 0x80 | ((code >> 6) & 0x3F);
	*t = 0x80 | (0x3F & code);
        } else if (0x03FFFFFF >= code) {
	*t++ = 0xF8 | (code >> 24);
	*t++ = 0x80 | ((code >> 18) & 0x3F);
	*t++ = 0x80 | ((code >> 12) & 0x3F);
	*t++ = 0x80 | ((code >> 6) & 0x3F);
	*t = 0x80 | (0x3F & code);
        } else if (0x7FFFFFFF >= code) {
	*t++ = 0xFC | (code >> 30);
	*t++ = 0x80 | ((code >> 24) & 0x3F);
	*t++ = 0x80 | ((code >> 18) & 0x3F);
	*t++ = 0x80 | ((code >> 12) & 0x3F);
	*t++ = 0x80 | ((code >> 6) & 0x3F);
	*t = 0x80 | (0x3F & code);
        } else {
            if (pi.has_error) {
                call_error("invalid Unicode", pi, __FILE__, __LINE__);
            }
            raise_error("invalid Unicode", pi.str, pi.s);
        }
        return t;
    }

// Assume the IRubyObject starts immediately and goes until the quote character is
 // reached again. Do not read the character after the terminating quote.
 //
    static String read_quoted_value(ParseInfo pi) {
    char	*value = 0;
    char	*h = pi.s; // head
    char	*t = h;	    // tail
        uint32_t	code;

        h++;	// skip quote character
        t++;
        value = h;
        for (; '"' != *h; h++, t++) {
            if ('\0' == *h) {
                pi.s = h;
                raise_error("quoted string not terminated", pi.str, pi.s);
            } else if ('\\' == *h) {
                h++;
                switch (*h) {
                    case 'n':	*t = '\n';	break;
                    case 'r':	*t = '\r';	break;
                    case 't':	*t = '\t';	break;
                    case 'f':	*t = '\f';	break;
                    case 'b':	*t = '\b';	break;
                    case '"':	*t = '"';	break;
                    case '/':	*t = '/';	break;
                    case '\\':	*t = '\\';	break;
                    case 'u':
                        h++;
                        code = read_hex(pi, h);
                        h += 3;
                        if (0x0000D800 <= code && code <= 0x0000DFFF) {
                            uint32_t	c1 = (code - 0x0000D800) & 0x000003FF;
                            uint32_t	c2;

                            h++;
                            if ('\\' != *h || 'u' != *(h + 1)) {
                                pi.s = h;
                                if (pi.has_error) {
                                    call_error("invalid escaped character", pi, __FILE__, __LINE__);
                                }
                                raise_error("invalid escaped character", pi.str, pi.s);
                            }
                            h += 2;
                            c2 = read_hex(pi, h);
                            h += 3;
                            c2 = (c2 - 0x0000DC00) & 0x000003FF;
                            code = ((c1 << 10) | c2) + 0x00010000;
                        }
                        t = unicode_to_chars(pi, t, code);
                        break;
                    default:
                        pi.s = h;
                        if (pi.has_error) {
                            call_error("invalid escaped character", pi, __FILE__, __LINE__);
                        }
                        raise_error("invalid escaped character", pi.str, pi.s);
                        break;
                }
            } else if (t != h) {
	    *t = *h;
            }
        }
    *t = '\0'; // terminate value
        pi.s = h + 1;

        return value;
    }

    void
    saj_parse(IRubyObject handler, char *json) {
        volatile IRubyObject	obj = context.nil;
        struct _ParseInfo	pi;

        if (0 == json) {
            if (pi.has_error) {
                call_error("Invalid arg, xml string can not be null", &pi, __FILE__, __LINE__);
            }
            raise_error("Invalid arg, xml string can not be null", json, 0);
        }
    // skip UTF-8 BOM if present
        if (0xEF == (uint8_t)*json && 0xBB == (uint8_t)json[1] && 0xBF == (uint8_t)json[2]) {
            json += 3;
        }
    // initialize parse info
        pi.str = json;
        pi.s = json;
        {
            struct rlimit	lim;

            if (0 == getrlimit(RLIMIT_STACK, &lim)) {
            pi.stack_min = (void*)((char*)&obj - (lim.rlim_cur / 4 * 3)); // let 3/4ths of the stack be used only
        } else {
            pi.stack_min = 0; // indicates not to check stack limit
        }
        }
        pi.handler = handler;
        pi.has_hash_start = rb_respond_to(handler, oj_hash_start_id);
        pi.has_hash_end = rb_respond_to(handler, oj_hash_end_id);
        pi.has_array_start = rb_respond_to(handler, oj_array_start_id);
        pi.has_array_end = rb_respond_to(handler, oj_array_end_id);
        pi.has_add_IRubyObject = rb_respond_to(handler, oj_add_IRubyObject_id);
        pi.has_error = rb_respond_to(handler, oj_error_id);
        read_next(&pi, 0);
        next_non_white(&pi);
        if ('\0' != *pi.s) {
            if (pi.has_error) {
                call_error("invalid format, extra characters", &pi, __FILE__, __LINE__);
            } else {
                raise_error("invalid format, extra characters", pi.str, pi.s);
            }
        }
    }

    // call-seq: saj_parse(handler, io)
    //
    // Parses an IO stream or file containing an JSON document. Raises an exception
    // if the JSON is malformed.
    // @param [Oj::Saj] handler Saj (responds to Oj::Saj methods) like handler
    // @param [IO|String] io IO Object to read from
    //
    IRubyObject oj_saj_parse(int argc, IRubyObject[] argv, IRubyObject self) {
    char	*json = 0;
        size_t	len = 0;
        IRubyObject	input = argv[1];

        if (argc < 2) {
            rb_raise(rb_eArgError, "Wrong number of arguments to saj_parse.\n");
        }
        if (rb_type(input) == T_STRING) {
            // the json string gets modified so make a copy of it
            len = RSTRING_LEN(input) + 1;
            json = ALLOC_N(char, len);
            strcpy(json, StringIRubyObjectPtr(input));
        } else {
            IRubyObject		clas = rb_obj_class(input);
            volatile IRubyObject	s;

            if (oj_stringio_class == clas) {
                s = rb_funcall2(input, oj_string_id, 0, 0);
                len = RSTRING_LEN(s) + 1;
                json = ALLOC_N(char, len);
                strcpy(json, rb_string_IRubyObject_cstr((IRubyObject*)&s));
            } else if (rb_cFile == clas && 0 == FIX2INT(rb_funcall(input, oj_pos_id, 0))) {
                int		fd = FIX2INT(rb_funcall(input, oj_fileno_id, 0));
                ssize_t	cnt;

                len = lseek(fd, 0, SEEK_END);
                lseek(fd, 0, SEEK_SET);
                json = ALLOC_N(char, len + 1);
                if (0 >= (cnt = read(fd, json, len)) || cnt != (ssize_t)len) {
                    rb_raise(rb_eIOError, "failed to read from IO Object.");
                }
                json[len] = '\0';
            } else if (rb_respond_to(input, oj_read_id)) {
                s = rb_funcall2(input, oj_read_id, 0, 0);
                len = RSTRING_LEN(s) + 1;
                json = ALLOC_N(char, len);
                strcpy(json, rb_string_IRubyObject_cstr((IRubyObject*)&s));
            } else {
                rb_raise(rb_eArgError, "saj_parse() expected a String or IO Object.");
            }
        }
        saj_parse(*argv, json);

        return context.nil;
    }
    */
}
