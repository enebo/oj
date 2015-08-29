package oj;

/**
 * Created by enebo on 8/28/15.
 */
public class Object {
    static void oj_set_object_callbacks(ParseInfo pi, OjLibrary oj) {
        Strict.oj_set_strict_callbacks(pi, oj);
        /*
        pi->end_hash = end_hash;
        pi->start_hash = start_hash;
        pi->hash_set_cstr = hash_set_cstr;
        pi->hash_set_num = hash_set_num;
        pi->hash_set_value = hash_set_value;
        pi->add_cstr = add_cstr;
        pi->add_num = add_num;
        pi->array_append_cstr = array_append_cstr;
        pi->array_append_num = array_append_num;
        */
    }
}
