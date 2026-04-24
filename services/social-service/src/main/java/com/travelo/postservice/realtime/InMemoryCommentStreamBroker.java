package com.travelo.postservice.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Same-JVM fan-out broker. {@link #publish} iterates a copy-on-write listener list
 * per post id; {@link #subscribe} returns a handle that removes the listener on close.
 *
 * <p>Listener exceptions are logged and swallowed so a misbehaving consumer can't
 * disrupt the comment write path or other subscribers.
 */
public class InMemoryCommentStreamBroker implements CommentStreamBroker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCommentStreamBroker.class);

    private final Map<String, List<Consumer<CommentStreamEvent>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public void publish(String postId, CommentStreamEvent event) {
        if (postId == null || postId.isBlank() || event == null) {
            return;
        }
        List<Consumer<CommentStreamEvent>> list = listeners.get(postId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Consumer<CommentStreamEvent> l : list) {
            try {
                l.accept(event);
            } catch (Exception e) {
                log.warn("in-memory comment broker listener failed postId={}: {}", postId, e.toString());
            }
        }
    }

    @Override
    public Subscription subscribe(String postId, Consumer<CommentStreamEvent> listener) {
        if (postId == null || postId.isBlank() || listener == null) {
            return () -> { /* no-op */ };
        }
        List<Consumer<CommentStreamEvent>> list =
                listeners.computeIfAbsent(postId, k -> new CopyOnWriteArrayList<>());
        list.add(listener);
        return () -> {
            List<Consumer<CommentStreamEvent>> current = listeners.get(postId);
            if (current != null) {
                current.remove(listener);
                if (current.isEmpty()) {
                    listeners.remove(postId, current);
                }
            }
        };
    }
}
