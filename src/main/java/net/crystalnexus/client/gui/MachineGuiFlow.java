package net.crystalnexus.client.gui;

import net.crystalnexus.world.inventory.MachineGuiLayout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

/** Orthogonal processing arrows; paths are checked against the actual control frames. */
public final class MachineGuiFlow {
    private static final int HEAD_LENGTH = 4;
    private MachineGuiFlow() {}
    public record Point(int x, int y) {}

    public static List<List<Point>> press(Rect first, Rect die, Rect fluid, Rect output) {
        int center = output.x() + output.width() / 2;
        int join = (first.bottom() + output.y()) / 2;
        int a = first.x() + first.width() / 2, b = die.x() + die.width() / 2;
        List<List<Point>> paths = new ArrayList<>();
        paths.add(List.of(new Point(a, first.bottom() + 3), new Point(a, join), new Point(center, join)));
        paths.add(List.of(new Point(b, die.bottom() + 3), new Point(b, join), new Point(center, join)));
        if (fluid != null) paths.add(List.of(new Point(fluid.x() + fluid.width() / 2, fluid.bottom() + 3),
                new Point(b, fluid.bottom() + 3), new Point(b, join), new Point(center, join)));
        paths.add(List.of(new Point(center, join), new Point(center, output.y() - 4)));
        return paths;
    }

    public record Route(Rect from, Rect to, int stage) {}

    /**
     * One processing cycle reads as one arrow: item inputs whose slots touch (a row, column or grid of inputs)
     * become a single source spanning the group. Combining machines keep one feed per input.
     */
    public static List<Route> mergeAdjacent(int[][] connections, java.util.function.IntFunction<Rect> endpoint, boolean merge) {
        List<Route> routes = new ArrayList<>();
        for (int[] connection : connections) {
            Rect from = endpoint.apply(connection[0]), to = endpoint.apply(connection[1]);
            if (from == null || to == null) continue;
            int stage = connection.length > 2 ? connection[2] : 0;
            Route route = new Route(from, to, stage);
            if (merge && connection[0] >= 0) {
                boolean grown = true;
                while (grown) {
                    grown = false;
                    for (int i = 0; i < routes.size(); i++) {
                        Route other = routes.get(i);
                        if (other.to().equals(route.to()) && other.stage() == route.stage() && touches(other.from(), route.from())) {
                            route = new Route(other.from().union(route.from()), route.to(), stage);
                            routes.remove(i); grown = true; break;
                        }
                    }
                }
            }
            routes.add(route);
        }
        return routes;
    }
    private static boolean touches(Rect a, Rect b) {
        return new Rect(a.x() - 1, a.y() - 1, a.width() + 2, a.height() + 2).intersects(b);
    }

    public static List<Point> path(Rect source, Rect target, List<Rect> obstacles, Rect area) {
        List<Point> best = List.of();
        int shortest = Integer.MAX_VALUE;
        int fewestBends = Integer.MAX_VALUE;
        for (Point a : ports(source)) for (Point b : ports(target)) {
            List<List<Point>> candidates = new ArrayList<>();
            candidates.add(List.of(a, new Point(b.x, a.y), b));
            candidates.add(List.of(a, new Point(a.x, b.y), b));
            List<Integer> columns = new ArrayList<>(List.of((a.x + b.x) / 2));
            List<Integer> rows = new ArrayList<>(List.of((a.y + b.y) / 2));
            for (Rect r : obstacles) {
                columns.add(r.x() - 8); columns.add(r.right() + 7);
                rows.add(r.y() - 8); rows.add(r.bottom() + 7);
            }
            for (int x : columns) candidates.add(List.of(a, new Point(x, a.y), new Point(x, b.y), b));
            for (int y : rows) candidates.add(List.of(a, new Point(a.x, y), new Point(b.x, y), b));
            Point from = outward(source, a), to = outward(target, b);
            if (from.x != 0 && to.x != 0) {
                int ax = a.x + from.x * 8, bx = b.x + to.x * 8;
                for (int y : rows) candidates.add(List.of(a, new Point(ax, a.y), new Point(ax, y), new Point(bx, y), new Point(bx, b.y), b));
            }
            if (from.y != 0 && to.y != 0) {
                int ay = a.y + from.y * 8, by = b.y + to.y * 8;
                for (int x : columns) candidates.add(List.of(a, new Point(a.x, ay), new Point(x, ay), new Point(x, by), new Point(b.x, by), b));
            }
            for (var candidate : candidates) {
                var points = simplify(candidate);
                if (points.size() < 2 || !leaves(source, points.getFirst(), points.get(1))
                        || !leaves(target, points.getLast(), points.get(points.size() - 2))) continue;
                int length = 0;
                boolean clear = true;
                for (int i = 1; i < points.size(); i++) {
                    Point p = points.get(i - 1), q = points.get(i);
                    Rect line = new Rect(Math.min(p.x, q.x) - 2, Math.min(p.y, q.y) - 2,
                            Math.abs(p.x - q.x) + 5, Math.abs(p.y - q.y) + 5);
                    if (!clear(line, obstacles, area)) { clear = false; break; }
                    length += Math.abs(p.x - q.x) + Math.abs(p.y - q.y);
                }
                Point end = points.getLast(), previous = points.get(points.size() - 2);
                int bends = points.size() - 2;
                if (clear && length >= 7 && Math.abs(end.x - previous.x) + Math.abs(end.y - previous.y) >= HEAD_LENGTH
                        && arrowHead(previous, end).stream().allMatch(r -> clear(r, obstacles, area))
                        && (bends < fewestBends || bends == fewestBends && length < shortest)) {
                    fewestBends = bends; shortest = length; best = points;
                }
            }
        }
        return best;
    }

    private static boolean clear(Rect r, List<Rect> obstacles, Rect area) {
        return r.x() >= area.x() && r.y() >= area.y() && r.right() <= area.right() && r.bottom() <= area.bottom()
            && obstacles.stream().noneMatch(r::intersects);
    }

    private static Point outward(Rect r, Point port) {
        return new Point(port.x < r.x() ? -1 : port.x >= r.right() ? 1 : 0,
            port.y < r.y() ? -1 : port.y >= r.bottom() ? 1 : 0);
    }

    private static boolean leaves(Rect r, Point port, Point next) {
        Point direction = outward(r, port);
        return direction.x != 0 ? next.y == port.y && Integer.signum(next.x - port.x) == direction.x
            : next.x == port.x && Integer.signum(next.y - port.y) == direction.y;
    }

    private static List<Point> simplify(List<Point> points) {
        List<Point> result = new ArrayList<>();
        for (Point point : points) {
            if (!result.isEmpty() && result.getLast().equals(point)) continue;
            while (result.size() >= 2) {
                Point a = result.get(result.size() - 2), b = result.getLast();
                if ((b.x - a.x) * (point.y - b.y) != (b.y - a.y) * (point.x - b.x)) break;
                if ((b.x - a.x) * (point.x - b.x) + (b.y - a.y) * (point.y - b.y) < 0) return List.of();
                result.removeLast();
            }
            result.add(point);
        }
        return result;
    }

    private static List<Point> ports(Rect r) {
        return List.of(new Point(r.right() + 3, r.y() + r.height() / 2),
                new Point(r.x() - 4, r.y() + r.height() / 2),
                new Point(r.x() + r.width() / 2, r.bottom() + 3),
                new Point(r.x() + r.width() / 2, r.y() - 4));
    }

    static List<Rect> arrowHead(Point previous, Point end) {
        int dx = Integer.signum(end.x - previous.x), dy = Integer.signum(end.y - previous.y);
        List<Rect> result = new ArrayList<>();
        for (int i = 0; i < HEAD_LENGTH; i++) {
            int x = end.x - dx * i, y = end.y - dy * i;
            result.add(dx == 0 ? new Rect(x - i - 1, y, (i + 1) * 2, 1)
                : new Rect(x, y - i - 1, 1, (i + 1) * 2));
        }
        return result;
    }

    public static void draw(GuiGraphics g, List<Point> points, boolean head) {
        var clean = simplify(points);
        if (clean.size() < 2) return;
        Point previous = clean.get(clean.size() - 2), end = clean.getLast();
        int color = 0xFF7896AD;
        for (int i = 1; i < clean.size(); i++) {
            Point p = clean.get(i - 1), q = clean.get(i);
            if (head && i == clean.size() - 1)
                q = new Point(q.x - Integer.signum(q.x - p.x) * (HEAD_LENGTH - 1), q.y - Integer.signum(q.y - p.y) * (HEAD_LENGTH - 1));
            g.fill(Math.min(p.x, q.x) - 1, Math.min(p.y, q.y) - 1, Math.max(p.x, q.x) + 1, Math.max(p.y, q.y) + 1, color);
        }
        if (head) for (Rect r : arrowHead(previous, end)) g.fill(r.x(), r.y(), r.right(), r.bottom(), color);
    }
}
