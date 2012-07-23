package org.motechproject.server.decisiontree;

import org.motechproject.decisiontree.model.ITransition;
import org.motechproject.decisiontree.model.Node;
import org.motechproject.decisiontree.model.Tree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Locates a tree node by transitionPath.
 */
@Component
public class TreeNodeLocator {
    public static final String PATH_DELIMITER = "/";
    public static final String ANY_KEY = "?";

    @Autowired
    ApplicationContext applicationContext;

    public TreeNodeLocator() {
    }

    public TreeNodeLocator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Node findNode(Tree tree, String path) {
        if (tree == null || path == null) {
            throw new IllegalArgumentException(String.format("tree: %s path: %s", tree, path));
        }
        Node node = tree.getRootNode();
        if (node != null) {
            String[] keys = path.split(PATH_DELIMITER);
            for (String key : keys) {
                if (key.isEmpty()) continue;
                ITransition transition = node.getTransitions().get(key);
                if (transition == null) transition = node.getTransitions().get(ANY_KEY);
                if (transition == null) return null;
                applicationContext.getAutowireCapableBeanFactory().autowireBean(transition); //TODO : autowiring in 2 places, see - DecistionTreeController
                node = transition.getDestinationNode(key);
                if (node == null) return null;
            }
        }
        return node;
    }
}
