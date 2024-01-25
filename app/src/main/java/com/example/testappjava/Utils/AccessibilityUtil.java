package com.example.testappjava.Utils;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityUtil {

    public static void findNodesByClassName(AccessibilityNodeInfo nodeInfo, String className, List<AccessibilityNodeInfo> nodes) {
        if (nodeInfo == null) {
            return;
        }

        if (nodeInfo.getClassName().toString().equals(className)) {
            nodes.add(nodeInfo);
        }

        final int childCount = nodeInfo.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            findNodesByClassName(childNode, className, nodes);
        }
    }

    public static AccessibilityNodeInfo findNodeByPackageName(AccessibilityNodeInfo node, String targetClassName) {
        if (node == null) return null;

        if (node.getPackageName().toString().equals(targetClassName)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            AccessibilityNodeInfo targetNode = findNodeByPackageName(childNode, targetClassName);
            if (targetNode != null) {
                return targetNode;
            }
        }

        return null;
    }

    public static AccessibilityNodeInfo getTopMostParentNode(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }

        AccessibilityNodeInfo parentNode = nodeInfo;
        AccessibilityNodeInfo topMostParentNode = null;

        while (parentNode != null) {
            topMostParentNode = parentNode;
            parentNode = parentNode.getParent();
        }

        return topMostParentNode;
    }

    public static String getAccessibilityNodeTreeAsXml(AccessibilityNodeInfo node) {
        StringBuilder xmlBuilder = new StringBuilder();
        traverseAccessibilityNode(node, xmlBuilder, 0);
        return xmlBuilder.toString();
    }

    public static void logLargeString(String tag, String message) {
        if (message.length() > 500) {
            int chunkCount = message.length() / 500; // Calculate how many chunks are needed
            for (int i = 0; i <= chunkCount; i++) {
                int max = 500 * (i + 1);
                if (max >= message.length()) {
                    Log.d(tag, "Chunk " + i + "/" + chunkCount + ": " + message.substring(500 * i));
                } else {
                    Log.d(tag, "Chunk " + i + "/" + chunkCount + ": " + message.substring(500 * i, max));
                }
            }
        } else {
            Log.d(tag, message);
        }
    }

    public static void traverseAccessibilityNode(AccessibilityNodeInfo node, StringBuilder xmlBuilder, int depth) {
        if (node == null) return;

        // Indentation based on depth for better readability
        for (int i = 0; i < depth; i++) {
            xmlBuilder.append("\t");
        }

        xmlBuilder.append("<node");
        xmlBuilder.append(" class=\"").append(node.getClassName()).append("\"");
        xmlBuilder.append(" package=\"").append(node.getPackageName()).append("\"");
        xmlBuilder.append(" text=\"").append(node.getText()).append("\"");
        xmlBuilder.append(" content-desc=\"").append(node.getContentDescription()).append("\"");

        int childCount = node.getChildCount();
        if (childCount > 0) {
            xmlBuilder.append(">\n");
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                traverseAccessibilityNode(childNode, xmlBuilder, depth + 1);
            }
            // Indentation before closing tag
            for (int i = 0; i < depth; i++) {
                xmlBuilder.append("\t");
            }
            xmlBuilder.append("</node>\n");
        } else {
            xmlBuilder.append(" />\n");
        }

        // Recycle the node to release resources
        node.recycle();
    }

    public static AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo rootNode, String text, boolean deepSearch, boolean clickable) {
        if (rootNode == null) return null;

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = rootNode.getChild(i);
            if (childNode != null) {
                if (childNode.getText() != null && text.equals(childNode.getText().toString()) && (!clickable || childNode.isClickable())) {
                    return childNode;
                } else if (deepSearch) {
                    if (childNode.getText() != null && childNode.getText().toString().contains(text) && (!clickable || childNode.isClickable())) {
                        return childNode;
                    }
                }
                if (childNode.getContentDescription() != null && text.equals(childNode.getContentDescription().toString()) && (!clickable || childNode.isClickable())) {
                    return childNode;
                } else if (deepSearch) {
                    if (childNode.getContentDescription() != null && childNode.getContentDescription().toString().contains(text) && (!clickable || childNode.isClickable())) {
                        return childNode;
                    }
                }
                AccessibilityNodeInfo foundNode = findNodeByText(childNode, text, deepSearch, clickable);
                childNode.recycle();
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo findNodeByClassName(AccessibilityNodeInfo node, String targetClassName) {
        if (node == null) return null;

        if (node.getClassName().toString().equals(targetClassName)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            AccessibilityNodeInfo targetNode = findNodeByClassName(childNode, targetClassName);
            if (targetNode != null) {
                return targetNode;
            }
        }

        return null;
    }

    public static AccessibilityNodeInfo findNodeByResourceId(AccessibilityNodeInfo node, String targetResourceIdName) {
        if (node == null) return null;

        if (node.getViewIdResourceName() != null) {
            if (node.getViewIdResourceName().equals(targetResourceIdName) || node.getViewIdResourceName().contains(targetResourceIdName))
                return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                AccessibilityNodeInfo childNode = node.getChild(i);
                AccessibilityNodeInfo targetNode = findNodeByResourceId(childNode, targetResourceIdName);
                if (targetNode != null) {
                    return targetNode;
                }
            } catch (Exception ignored) {

            }

        }

        return null;
    }

    public static List<String> listAllTextsInActiveWindow(AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            List<String> allTexts = new ArrayList<>();
            traverseNodesForText(rootNode, allTexts);
            rootNode.recycle();
            // Now 'allTexts' contains a list of all texts in the active window
            Gson gson = new Gson();
            String json = gson.toJson(allTexts);
            Log.d("OUTPUT", json);
            return allTexts;
        } else {
            Log.d("OUTPUT", "[]");
        }
        return new ArrayList<>();
    }

    public static void traverseNodesForText(AccessibilityNodeInfo node, List<String> allTexts) {
        if (node == null) return;
        String output = node.getText() != null ? node.getText().toString() : node.getContentDescription() != null ? node.getContentDescription().toString() : "";
        allTexts.add(output);

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            traverseNodesForText(childNode, allTexts);
        }
    }
}
