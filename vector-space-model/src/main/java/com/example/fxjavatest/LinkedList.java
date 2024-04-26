package com.example.fxjavatest;
import javafx.util.Pair;

import java.util.ArrayList;

class Node {
    Pair data;
    Node next;
    Node(long docId, long termFrequency) {
        data = new Pair(docId, termFrequency);
        this.next = null;
    }
}

//public class LinkedList {
//    public Node head;
//
//    public LinkedList() {
//        this.head = null;
//    }
//    public void insert(long docId, long termFrequency) {
//        Node newNode = new Node(docId,termFrequency);
//
//        // If the list is empty or new data is smaller than the head
//        if(head == null)
//        {
//            head = newNode;
//            return;
//        }
//
////        if(data < head.data)
////        {
////
////        }
//        if (docId < head.data.getKey()) {
//            newNode.next = head;
//            head = newNode;
//            return;
//        }
//
//        if(data == head.data)
//            return;
//        Node current = head;
//        // Find the position to insert the new node
//        while (current.next != null && current.next.data < data) {
//            current = current.next;
//        }
//
//        if (current.next == null && current.data == data)
//            return;
//
//        if (current.next != null && current.next.data == data)
//            return;
//
//        // Insert the new node after current
//        newNode.next = current.next;
//        current.next = newNode;
//    }
//
//    public long size() {
//        long count = 0;
//        Node current = head;
//        while (current != null) {
//            count++;
//            current = current.next;
//        }
//        return count;
//    }
//
//    public void display() {
//        Node current = head;
//        while (current != null) {
//            System.out.print(current.data + " ");
//            current = current.next;
//        }
//        System.out.println();
//    }
//    public ArrayList<Long> convertToArrayList()
//    {
//        ArrayList<Long> ans = new ArrayList<>();
//        Node temp = this.head;
//        while(temp != null)
//        {
//            ans.add(temp.data);
//            temp = temp.next;
//        }
//
//        return ans;
//    }
//}