package com.iitsm.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Represents an IT service within the organization (e.g., "Email", "CRM", "VPN").
 * <p>
 * Services can declare upstream dependencies, forming a directed graph.
 * The {@link #getBlastRadius()} method performs a BFS traversal to determine
 * all downstream services that would be affected if this service goes down.
 * </p>
 */
@Entity
@Table(name = "it_services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    /**
     * Business criticality rating from 1 (lowest) to 5 (highest).
     */
    private int businessCriticality;

    @ManyToMany
    @JoinTable(
            name = "service_dependencies",
            joinColumns = @JoinColumn(name = "downstream_id"),
            inverseJoinColumns = @JoinColumn(name = "upstream_id")
    )
    private Set<ITService> upstreamDependencies = new HashSet<>();

    @ManyToMany(mappedBy = "upstreamDependencies")
    private Set<ITService> downstreamDependencies = new HashSet<>();

    public ITService(String name, String description, int businessCriticality) {
        this.name = name;
        this.description = description;
        this.businessCriticality = businessCriticality;
    }

    /**
     * Adds an upstream dependency to this service.
     *
     * @param service the upstream service this service depends on
     */
    public void addUpstreamDependency(ITService service) {
        upstreamDependencies.add(service);
    }

    /**
     * Removes an upstream dependency from this service.
     *
     * @param service the upstream service to remove
     */
    public void removeUpstreamDependency(ITService service) {
        upstreamDependencies.remove(service);
    }

    /**
     * Computes the blast radius — the set of all downstream services that would be
     * transitively affected if this service experiences an outage.
     * <p>
     * Uses BFS traversal starting from this service's direct downstream dependents,
     * expanding through their downstream dependents recursively.
     * </p>
     *
     * @return an unmodifiable set of all transitively affected downstream services
     */
    public Set<ITService> getBlastRadius() {
        return Collections.unmodifiableSet(downstreamParents().keySet());
    }

    /** One shortest outage propagation path per downstream service, including both ends. */
    public List<List<ITService>> getImpactPaths() {
        Map<ITService, ITService> parents = downstreamParents();
        List<List<ITService>> paths = new ArrayList<>();
        for (ITService destination : parents.keySet()) {
            Deque<ITService> path = new ArrayDeque<>();
            for (ITService node = destination; node != null; node = parents.get(node)) {
                path.addFirst(node);
            }
            paths.add(List.copyOf(path));
        }
        return List.copyOf(paths);
    }

    private Map<ITService, ITService> downstreamParents() {
        Map<ITService, ITService> parents = new LinkedHashMap<>();
        Deque<ITService> queue = new ArrayDeque<>();
        parents.put(this, this);
        queue.add(this);
        while (!queue.isEmpty()) {
            ITService current = queue.remove();
            // Stable tie-break when several paths reach the same service.
            for (ITService next : current.getDownstreamDependencies().stream()
                    .sorted(Comparator.comparing(ITService::getName)).toList()) {
                if (parents.putIfAbsent(next, current) == null) {
                    queue.add(next);
                }
            }
        }
        parents.remove(this);
        return parents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ITService that && id != null && Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ITService{id=" + id + ", name='" + name + "', criticality=" + businessCriticality + "}";
    }
}
