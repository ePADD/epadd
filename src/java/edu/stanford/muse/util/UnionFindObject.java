/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.util;

import java.io.*;

// an object for fast union finds
public class UnionFindObject implements Serializable
{

    private UnionFindObject _parent;
    private int _rank;

    public UnionFindObject ()
    {
        reset();
    }

    public void reset()
    {
        _parent = this;
        _rank = 0;
    }

    public UnionFindObject find ()
    {
        UnionFindObject root = this;
        while (root != root._parent)
        {
            root = root._parent;

            // go back up the tree, setting each node's parent to root
        }
        UnionFindObject tmp = this;
        while (tmp != root)
        {
            UnionFindObject parent = tmp._parent;
            tmp._parent = root;
            tmp = parent;
        }

        return root;
    }

    public void unify (UnionFindObject o)
    {
        if (o == null)
        {
            return;
        }
        this.find ().link (o.find ());
    }

    // Brute force set of equiv. class. Use with care.
    public void set_class(UnionFindObject o)
    {
        this._parent = o;
    }

    private void link (UnionFindObject o)
    {
        if (this._rank > o._rank)
        {
            o._parent = this;
        }
        else
        {
            this._parent = o;
            if (this._rank == o._rank)
            {
                o._rank++;
            }
        }
    }

}
