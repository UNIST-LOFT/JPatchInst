package kr.ac.unist.apr.gumtree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.RootsClassifier;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class MyRootsClassifier extends RootsClassifier{
    public MyRootsClassifier(TreeContext src, TreeContext dst, Set<Mapping> rawMappings, List<Action> script) {
        super(src, dst, rawMappings, script);
    }

    public MyRootsClassifier(TreeContext src, TreeContext dst, Matcher m) {
        super(src, dst, m);
    }

    @Override
    public void classify() {
        Set<Tree> insertedDsts = new HashSet<>();
        for (Action a: actions)
            if (a instanceof Insert)
                dstAddTrees.add(a.getNode());

        Set<Tree> deletedSrcs = new HashSet<>();
        for (Action a: actions)
            if (a instanceof Delete)
                srcDelTrees.add(a.getNode());

        for (Action a: actions) {
            if (a instanceof Delete) {
                if (!(deletedSrcs.containsAll(a.getNode().getDescendants())
                        && deletedSrcs.contains(a.getNode().getParent())))
                    srcDelTrees.add(a.getNode());
            }
            else if (a instanceof Insert) {
                if (!(insertedDsts.containsAll(a.getNode().getDescendants())
                        && insertedDsts.contains(a.getNode().getParent())))
                    dstAddTrees.add(a.getNode());
            }
            else if (a instanceof Update) {
                srcUpdTrees.add(a.getNode());
                dstUpdTrees.add(mappings.getDst(a.getNode()));
            }
            else if (a instanceof Move) {
                srcMvTrees.add(a.getNode());
                dstMvTrees.add(mappings.getDst(a.getNode()));
            }
        }
    }
}
