package dev.flomik.ponderlib.foundation.registration;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import dev.flomik.ponderlib.api.registration.StoryBoardEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PonderSceneRegistry {

    private final Multimap<ResourceLocation, StoryBoardEntry> entries = LinkedHashMultimap.create();

    public void register(StoryBoardEntry entry) {
        entries.put(entry.getComponent(), entry);
    }

    public boolean doScenesExistForId(ResourceLocation component) {
        return entries.containsKey(component);
    }

    /**
     * Every registered entry across every component, in registration order - the source list for
     * {@code foundation.ui.PonderIndexScreen}'s scene browser.
     */
    public Collection<StoryBoardEntry> getAllEntries() {
        return entries.values();
    }

    /**
     * Registration order, topologically sorted by any {@link StoryBoardEntry#getOrderBefore()}/
     * {@link StoryBoardEntry#getOrderAfter()} constraints among the entries for this component -
     * see {@link #orderEntries}. Entries with no such constraints (the common case - both of
     * PonderLib's own demo scenes, for instance) just keep their registration order.
     */
    public List<StoryBoardEntry> getScenes(ResourceLocation component) {
        return orderEntries(new ArrayList<>(entries.get(component)));
    }

    /**
     * Every registered entry (across every component) carrying {@code tag} - the source lookup for
     * {@code foundation.ui.PonderIndexScreen}'s tag-filtered view.
     */
    public List<StoryBoardEntry> getScenesByTag(ResourceLocation tag) {
        List<StoryBoardEntry> result = new ArrayList<>();
        for (StoryBoardEntry entry : entries.values()) {
            if (entry.getTags().contains(tag)) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Every distinct tag carried by any registered entry, sorted by namespace then path
     * ({@link ResourceLocation}'s natural ordering) - the source list for {@code
     * foundation.ui.PonderTagIndexScreen}, which groups consecutive same-namespace tags under one
     * header. Sorting first means that grouping needs no separate {@code Map} - the tags for one
     * namespace are already clustered together in the sorted list.
     */
    public List<ResourceLocation> getAllTags() {
        Set<ResourceLocation> tags = new TreeSet<>();
        for (StoryBoardEntry entry : entries.values()) {
            tags.addAll(entry.getTags());
        }
        return new ArrayList<>(tags);
    }

    /**
     * Stable topological sort (Kahn's algorithm, FIFO tie-breaking so unconstrained entries keep
     * their registration order) over {@code orderBefore}/{@code orderAfter}, both keyed by
     * {@link StoryBoardEntry#getSchematicLocation()}. References to an entry outside this
     * component's own list are silently ignored (see {@link StoryBoardEntry#getOrderBefore()}) -
     * so is a cycle: instead of failing registration, whatever's left unresolved once no more
     * zero-dependency entries remain is just appended in registration order.
     */
    private static List<StoryBoardEntry> orderEntries(List<StoryBoardEntry> entries) {
        if (entries.size() <= 1) {
            return entries;
        }

        Map<ResourceLocation, StoryBoardEntry> byId = new LinkedHashMap<>();
        for (StoryBoardEntry entry : entries) {
            byId.putIfAbsent(entry.getSchematicLocation(), entry);
        }

        Map<ResourceLocation, Integer> inDegree = new LinkedHashMap<>();
        Map<ResourceLocation, List<ResourceLocation>> successors = new LinkedHashMap<>();
        for (ResourceLocation id : byId.keySet()) {
            inDegree.put(id, 0);
            successors.put(id, new ArrayList<>());
        }
        for (StoryBoardEntry entry : entries) {
            ResourceLocation id = entry.getSchematicLocation();
            for (ResourceLocation before : entry.getOrderBefore()) {
                addEdge(successors, inDegree, byId, id, before);
            }
            for (ResourceLocation after : entry.getOrderAfter()) {
                addEdge(successors, inDegree, byId, after, id);
            }
        }

        Deque<ResourceLocation> ready = new ArrayDeque<>();
        for (ResourceLocation id : byId.keySet()) {
            if (inDegree.get(id) == 0) {
                ready.add(id);
            }
        }

        List<StoryBoardEntry> sorted = new ArrayList<>();
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        while (!ready.isEmpty()) {
            ResourceLocation id = ready.poll();
            if (!visited.add(id)) {
                continue;
            }
            sorted.add(byId.get(id));
            for (ResourceLocation next : successors.get(id)) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }

        for (StoryBoardEntry entry : entries) {
            if (visited.add(entry.getSchematicLocation())) {
                sorted.add(entry);
            }
        }
        return sorted;
    }

    private static void addEdge(Map<ResourceLocation, List<ResourceLocation>> successors,
                                 Map<ResourceLocation, Integer> inDegree,
                                 Map<ResourceLocation, StoryBoardEntry> byId,
                                 ResourceLocation from, ResourceLocation to) {
        if (from.equals(to) || !byId.containsKey(from) || !byId.containsKey(to)) {
            return;
        }
        successors.get(from).add(to);
        inDegree.merge(to, 1, Integer::sum);
    }
}
