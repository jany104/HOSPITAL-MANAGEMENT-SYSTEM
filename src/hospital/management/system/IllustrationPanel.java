package hospital.management.system;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IllustrationPanel extends JPanel {

    public enum IllustrationStyle {
        HOSPITAL,
        PATIENT,
        ROOM,
        AMBULANCE,
        UPDATE,
        DASHBOARD,
        ADMISSION,
        DISCHARGE,
        SEARCH,
        LOGOUT,
        STAFF,
    DEPARTMENT,
    HERO
    }

    private static final Map<IllustrationStyle, String> RESOURCE_BASE_NAMES = Map.ofEntries(
        Map.entry(IllustrationStyle.HOSPITAL, "hospital"),
        Map.entry(IllustrationStyle.PATIENT, "patient-svgrepo-com"),
        Map.entry(IllustrationStyle.ROOM, "room"),
        Map.entry(IllustrationStyle.AMBULANCE, "ambulance"),
        Map.entry(IllustrationStyle.UPDATE, "update"),
        Map.entry(IllustrationStyle.DASHBOARD, "collection-svgrepo-com"),
        Map.entry(IllustrationStyle.ADMISSION, "admission"),
        Map.entry(IllustrationStyle.DISCHARGE, "discharge"),
        Map.entry(IllustrationStyle.SEARCH, "search-status-svgrepo-com"),
        Map.entry(IllustrationStyle.LOGOUT, "logout-svgrepo-com"),
            Map.entry(IllustrationStyle.STAFF, "staff"),
        Map.entry(IllustrationStyle.DEPARTMENT, "department-svgrepo-com"),
        Map.entry(IllustrationStyle.HERO, "rod-of-asclepius-icon-512877-512")
    );

    private static final Map<IllustrationStyle, IllustrationAsset> ASSETS = loadAssets();

    private final IllustrationStyle style;

    public IllustrationPanel(IllustrationStyle style) {
        this.style = style;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 320));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        GradientPaint background = new GradientPaint(0, 0, UITheme.PRIMARY_LIGHT, width, height, UITheme.PRIMARY);
        g2.setPaint(background);
        g2.fillRoundRect(0, 0, width, height, 48, 48);

        g2.setComposite(AlphaComposite.SrcOver.derive(0.14f));
        g2.setColor(UITheme.PRIMARY_DARK);
        g2.fillRoundRect(6, 10, Math.max(0, width - 12), Math.max(0, height - 16), 40, 40);

        g2.setComposite(AlphaComposite.SrcOver);
        GradientPaint highlight = new GradientPaint(0, 0, new Color(255, 255, 255, 120), 0, height, new Color(255, 255, 255, 10));
        g2.setPaint(highlight);
        g2.fillRoundRect(2, 2, Math.max(0, width - 4), Math.max(0, height - 4), 44, 44);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, 1, Math.max(0, width - 2), Math.max(0, height - 2), 46, 46);

        IllustrationAsset asset = ASSETS.getOrDefault(style, ASSETS.get(IllustrationStyle.HOSPITAL));
        double artWidth = asset.width();
        double artHeight = asset.height();
        double scale = Math.min((width - 32.0) / artWidth, (height - 32.0) / artHeight);
        if (!Double.isFinite(scale) || scale <= 0) {
            g2.dispose();
            return;
        }
        double translateX = (width - artWidth * scale) / 2.0;
        double translateY = (height - artHeight * scale) / 2.0;

        AffineTransform transform = new AffineTransform();
        transform.translate(translateX, translateY);
        transform.scale(scale, scale);

        if (asset.image() != null) {
            Object previousInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(asset.image(), transform, this);
            if (previousInterpolation != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
            }
        } else {
            for (SvgShape shape : asset.shapes()) {
                Shape transformed = transform.createTransformedShape(shape.shape());
                if (shape.fill() != null) {
                    g2.setPaint(shape.fill());
                    g2.fill(transformed);
                }
                if (shape.stroke() != null && shape.strokeWidth() > 0) {
                    g2.setStroke(new BasicStroke(shape.strokeWidth() * (float) scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setPaint(shape.stroke());
                    g2.draw(transformed);
                }
            }
        }

        g2.dispose();
    }

    private record IllustrationAsset(double width, double height, List<SvgShape> shapes, BufferedImage image) {
        private static final List<SvgShape> EMPTY_SHAPES = List.of();

        IllustrationAsset {
            shapes = shapes == null ? EMPTY_SHAPES : List.copyOf(shapes);
        }
    }

    private record SvgShape(Shape shape, Color fill, Color stroke, float strokeWidth) {
    }

    private static Map<IllustrationStyle, IllustrationAsset> loadAssets() {
        Map<IllustrationStyle, IllustrationAsset> assets = new EnumMap<>(IllustrationStyle.class);
        for (IllustrationStyle style : IllustrationStyle.values()) {
            String baseName = RESOURCE_BASE_NAMES.getOrDefault(style, style.name().toLowerCase());
            IllustrationAsset asset = null;

        List<String> candidateResources = List.of(
            "/hospital/management/system/illustrations/" + baseName + ".png",
            "/hospital/management/system/illustrations/" + baseName + ".svg"
        );

            List<String> failureMessages = new ArrayList<>();
            for (String resource : candidateResources) {
                try {
                    asset = resource.endsWith(".svg")
                            ? SvgLoader.load(resource)
                            : RasterLoader.load(resource);
                    if (asset != null) {
                        break;
                    }
                } catch (Exception ex) {
                    failureMessages.add(ex.getMessage());
                }
            }

            if (asset != null) {
                assets.put(style, asset);
            } else if (!failureMessages.isEmpty()) {
                System.err.println("Unable to load illustration asset: " + baseName + " - " + String.join("; ", failureMessages));
            }
        }

        if (assets.isEmpty()) {
            throw new IllegalStateException("No illustration assets could be loaded. Verify illustration resources are present.");
        }

        // Ensure every style has a value, fall back to the first available asset if needed.
        IllustrationAsset fallback = assets.values().iterator().next();
        for (IllustrationStyle style : IllustrationStyle.values()) {
            assets.putIfAbsent(style, fallback);
        }
        return assets;
    }

    private static final class SvgLoader {
        private static IllustrationAsset load(String resource) throws Exception {
            try (InputStream inputStream = IllustrationPanel.class.getResourceAsStream(resource)) {
                if (inputStream == null) {
                    throw new IOException("Resource not found: " + resource);
                }
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);

                String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                xml = stripDoctype(xml);
                Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                document.getDocumentElement().normalize();
                Element svgElement = document.getDocumentElement();

                double width = parseLength(svgElement.getAttribute("width"));
                double height = parseLength(svgElement.getAttribute("height"));
                String viewBox = svgElement.getAttribute("viewBox");
                if (!viewBox.isBlank()) {
                    String[] parts = viewBox.trim().split("\\s+");
                    if (parts.length == 4) {
                        width = Double.parseDouble(parts[2]);
                        height = Double.parseDouble(parts[3]);
                    }
                }
                if (width <= 0 || height <= 0) {
                    width = 256;
                    height = 256;
                }

                List<SvgShape> shapes = new ArrayList<>();
                NodeList children = svgElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    shapes.addAll(parseElement((Element) node));
                }

                return new IllustrationAsset(width, height, shapes, null);
            }
        }

        private static String stripDoctype(String xml) {
            return xml.replaceAll("(?is)<!DOCTYPE[^>]*>", "");
        }

        private static List<SvgShape> parseElement(Element el) {
            return switch (el.getTagName()) {
                case "path" -> List.of(parsePath(el));
                case "circle" -> List.of(parseCircle(el));
                case "ellipse" -> List.of(parseEllipse(el));
                case "rect" -> List.of(parseRect(el));
                case "line" -> List.of(parseLine(el));
                case "polyline" -> List.of(parsePolyline(el));
                case "polygon" -> List.of(parsePolygon(el));
                case "g" -> parseGroup(el);
                default -> List.of();
            };
        }

        private static List<SvgShape> parseGroup(Element group) {
            List<SvgShape> shapes = new ArrayList<>();
            NodeList children = group.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                shapes.addAll(parseElement((Element) node));
            }
            return shapes;
        }

        private static SvgShape parsePath(Element el) {
            String data = el.getAttribute("d");
            if (data == null || data.isBlank()) {
                throw new IllegalArgumentException("Path element missing 'd' attribute");
            }
            return createShape(el, SvgPathParser.parse(data), true);
        }

        private static SvgShape parseCircle(Element el) {
            double cx = parseLength(el.getAttribute("cx"));
            double cy = parseLength(el.getAttribute("cy"));
            double r = parseLength(el.getAttribute("r"));
            if (r <= 0) {
                throw new IllegalArgumentException("Circle element missing or invalid radius");
            }
            Shape shape = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
            return createShape(el, shape, true);
        }

        private static SvgShape parseEllipse(Element el) {
            double cx = parseLength(el.getAttribute("cx"));
            double cy = parseLength(el.getAttribute("cy"));
            double rx = parseLength(el.getAttribute("rx"));
            double ry = parseLength(el.getAttribute("ry"));
            if (rx <= 0 || ry <= 0) {
                throw new IllegalArgumentException("Ellipse element missing or invalid radii");
            }
            Shape shape = new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
            return createShape(el, shape, true);
        }

        private static SvgShape parseRect(Element el) {
            double x = parseLength(el.getAttribute("x"));
            double y = parseLength(el.getAttribute("y"));
            double width = parseLength(el.getAttribute("width"));
            double height = parseLength(el.getAttribute("height"));
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Rect element missing or invalid dimensions");
            }
            double rx = parseLength(attributeOrStyle(el, "rx", "rx"));
            double ry = parseLength(attributeOrStyle(el, "ry", "ry"));
            Shape shape;
            if (rx > 0 || ry > 0) {
                if (rx <= 0) {
                    rx = ry;
                }
                if (ry <= 0) {
                    ry = rx;
                }
                shape = new RoundRectangle2D.Double(x, y, width, height, rx * 2, ry * 2);
            } else {
                shape = new Rectangle2D.Double(x, y, width, height);
            }
            return createShape(el, shape, true);
        }

        private static SvgShape parseLine(Element el) {
            double x1 = parseLength(el.getAttribute("x1"));
            double y1 = parseLength(el.getAttribute("y1"));
            double x2 = parseLength(el.getAttribute("x2"));
            double y2 = parseLength(el.getAttribute("y2"));
            Shape shape = new Line2D.Double(x1, y1, x2, y2);
            return createShape(el, shape, false);
        }

        private static SvgShape parsePolyline(Element el) {
            double[] points = parsePoints(el.getAttribute("points"));
            if (points.length < 4) {
                throw new IllegalArgumentException("Polyline element requires at least two points");
            }
            Path2D.Double path = new Path2D.Double();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            return createShape(el, path, false);
        }

        private static SvgShape parsePolygon(Element el) {
            double[] points = parsePoints(el.getAttribute("points"));
            if (points.length < 6) {
                throw new IllegalArgumentException("Polygon element requires at least three points");
            }
            Path2D.Double path = new Path2D.Double();
            path.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) {
                path.lineTo(points[i], points[i + 1]);
            }
            path.closePath();
            return createShape(el, path, true);
        }

        private static SvgShape createShape(Element el, Shape shape, boolean defaultFill) {
            Color fill = harmonizeColor(parseColor(attributeOrStyle(el, "fill", "fill")), true);
            Color stroke = harmonizeColor(parseColor(attributeOrStyle(el, "stroke", "stroke")), false);
            float strokeWidth = (float) parseLength(attributeOrStyle(el, "stroke-width", "stroke-width"));

            String fillOpacity = attributeOrStyle(el, "fill-opacity", "fill-opacity");
            String strokeOpacity = attributeOrStyle(el, "stroke-opacity", "stroke-opacity");
            String opacity = attributeOrStyle(el, "opacity", "opacity");

            fill = applyOpacity(fill, fillOpacity);
            fill = applyOpacity(fill, opacity);
            stroke = applyOpacity(stroke, strokeOpacity);
            stroke = applyOpacity(stroke, opacity);

            if (stroke != null && strokeWidth <= 0f) {
                strokeWidth = 1.5f;
            }

            if (fill == null && stroke == null) {
                if (defaultFill) {
                    fill = UITheme.PRIMARY_LIGHT;
                } else {
                    stroke = UITheme.PRIMARY_LIGHT;
                    if (strokeWidth <= 0f) {
                        strokeWidth = 1.5f;
                    }
                }
            }

            return new SvgShape(shape, fill, stroke, strokeWidth);
        }

        private static Color applyOpacity(Color color, String opacityValue) {
            if (color == null || opacityValue == null || opacityValue.isBlank()) {
                return color;
            }
            try {
                double opacity = Double.parseDouble(opacityValue.trim());
                if (opacity < 0) {
                    opacity = 0;
                } else if (opacity > 1) {
                    opacity = 1;
                }
                int alpha = (int) Math.round(color.getAlpha() * opacity);
                return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            } catch (NumberFormatException ex) {
                return color;
            }
        }

        private static String attributeOrStyle(Element el, String attribute, String styleKey) {
            return firstNonBlank(el.getAttribute(attribute), el.getAttribute("style"), styleKey);
        }

        private static double[] parsePoints(String raw) {
            if (raw == null || raw.isBlank()) {
                return new double[0];
            }
            String[] tokens = raw.trim().split("[\\s,]+");
            double[] values = new double[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                try {
                    values[i] = Double.parseDouble(tokens[i]);
                } catch (NumberFormatException ex) {
                    values[i] = 0;
                }
            }
            if (values.length % 2 != 0) {
                double[] even = new double[values.length - 1];
                System.arraycopy(values, 0, even, 0, even.length);
                return even;
            }
            return values;
        }

        private static String firstNonBlank(String primary, String styleAttr, String styleKey) {
            if (primary != null && !primary.isBlank()) {
                return primary;
            }
            if (styleAttr != null && !styleAttr.isBlank()) {
                String[] declarations = styleAttr.split(";");
                for (String declaration : declarations) {
                    String[] parts = declaration.split(":");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (key.equalsIgnoreCase(styleKey)) {
                            return value;
                        }
                    }
                }
            }
            return null;
        }

        private static double parseLength(String attr) {
            if (attr == null || attr.isBlank()) {
                return 0;
            }
            String cleaned = attr.trim().replace("px", "");
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        private static Color parseColor(String value) {
            if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) {
                return null;
            }
            String normalized = value.trim();
            if (normalized.startsWith("#")) {
                return decodeHexColor(normalized.substring(1));
            }
            return switch (normalized.toLowerCase()) {
                case "white" -> Color.WHITE;
                case "black" -> Color.BLACK;
                default -> UITheme.PRIMARY_LIGHT;
            };
        }

        private static Color harmonizeColor(Color color, boolean isFill) {
            if (color == null) {
                return null;
            }
            double luminance = 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
            if (luminance < 80) {
                return isFill ? UITheme.PRIMARY_DARK : UITheme.PRIMARY_LIGHT;
            }
            if (luminance > 235) {
                return isFill ? new Color(255, 255, 255, color.getAlpha()) : new Color(220, 220, 220, color.getAlpha());
            }
            return color;
        }

        private static Color decodeHexColor(String hex) {
            if (hex.length() == 3) {
                String r = hex.substring(0, 1);
                String g = hex.substring(1, 2);
                String b = hex.substring(2, 3);
                hex = r + r + g + g + b + b;
            }
            int alpha = 255;
            if (hex.length() == 8) {
                alpha = Integer.parseInt(hex.substring(0, 2), 16);
                hex = hex.substring(2);
            }
            if (hex.length() != 6) {
                throw new IllegalArgumentException("Unsupported hex color length: " + hex);
            }
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return new Color(r, g, b, alpha);
        }
    }

    private static final class RasterLoader {
        private static IllustrationAsset load(String resource) throws IOException {
            try (InputStream inputStream = IllustrationPanel.class.getResourceAsStream(resource)) {
                if (inputStream == null) {
                    throw new IOException("Resource not found: " + resource);
                }
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new IOException("Unable to decode raster image: " + resource);
                }
                return new IllustrationAsset(image.getWidth(), image.getHeight(), List.of(), image);
            }
        }
    }

    private static class SvgPathParser {
        private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?");

        private static Path2D parse(String data) {
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO, 64);
            if (data == null || data.isBlank()) {
                return path;
            }

            double currentX = 0;
            double currentY = 0;
            double subPathStartX = 0;
            double subPathStartY = 0;
            double lastControlX = 0;
            double lastControlY = 0;
            char lastCommand = 0;
            for (String segment : splitSegments(data)) {
                if (segment.isEmpty()) {
                    continue;
                }
                char rawCommand = segment.charAt(0);
                boolean relative = Character.isLowerCase(rawCommand);
                char command = Character.toUpperCase(rawCommand);
                double[] values = parseNumbers(segment.substring(1));
                switch (command) {
                    case 'M' -> {
                        for (int i = 0; i < values.length; i += 2) {
                            double x = values[i];
                            double y = values[i + 1];
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }
                            currentX = x;
                            currentY = y;
                            if (i == 0) {
                                path.moveTo(currentX, currentY);
                                subPathStartX = currentX;
                                subPathStartY = currentY;
                            } else {
                                path.lineTo(currentX, currentY);
                            }
                        }
                    }
                    case 'L' -> {
                        for (int i = 0; i < values.length; i += 2) {
                            double x = values[i];
                            double y = values[i + 1];
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }
                            currentX = x;
                            currentY = y;
                            path.lineTo(currentX, currentY);
                        }
                    }
                    case 'C' -> {
                        for (int i = 0; i < values.length; i += 6) {
                            double x1 = values[i];
                            double y1 = values[i + 1];
                            double x2 = values[i + 2];
                            double y2 = values[i + 3];
                            double x = values[i + 4];
                            double y = values[i + 5];
                            if (relative) {
                                x1 += currentX;
                                y1 += currentY;
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }
                            currentX = x;
                            currentY = y;
                            lastControlX = x2;
                            lastControlY = y2;
                            path.curveTo(x1, y1, x2, y2, currentX, currentY);
                        }
                    }
                    case 'S' -> {
                        for (int i = 0; i < values.length; i += 4) {
                            double x2 = values[i];
                            double y2 = values[i + 1];
                            double x = values[i + 2];
                            double y = values[i + 3];
                            double x1;
                            double y1;
                            if (lastCommand == 'C' || lastCommand == 'S') {
                                x1 = currentX * 2 - lastControlX;
                                y1 = currentY * 2 - lastControlY;
                            } else {
                                x1 = currentX;
                                y1 = currentY;
                            }
                            if (relative) {
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }
                            currentX = x;
                            currentY = y;
                            lastControlX = x2;
                            lastControlY = y2;
                            path.curveTo(x1, y1, x2, y2, currentX, currentY);
                        }
                    }
                    case 'Q' -> {
                        for (int i = 0; i < values.length; i += 4) {
                            double x1 = values[i];
                            double y1 = values[i + 1];
                            double x = values[i + 2];
                            double y = values[i + 3];
                            if (relative) {
                                x1 += currentX;
                                y1 += currentY;
                                x += currentX;
                                y += currentY;
                            }
                            currentX = x;
                            currentY = y;
                            path.quadTo(x1, y1, currentX, currentY);
                        }
                    }
                    case 'H' -> {
                        for (double value : values) {
                            currentX = relative ? currentX + value : value;
                            path.lineTo(currentX, currentY);
                        }
                    }
                    case 'V' -> {
                        for (double value : values) {
                            currentY = relative ? currentY + value : value;
                            path.lineTo(currentX, currentY);
                        }
                    }
                    case 'Z' -> {
                        path.closePath();
                        currentX = subPathStartX;
                        currentY = subPathStartY;
                        lastControlX = currentX;
                        lastControlY = currentY;
                    }
                    default -> throw new IllegalArgumentException("Unsupported SVG command: " + command);
                }
                switch (command) {
                    case 'C', 'S' -> {
                        // lastControlX/Y already set during processing
                    }
                    default -> {
                        lastControlX = currentX;
                        lastControlY = currentY;
                    }
                }
                lastCommand = command;
            }
            return path;
        }

        private static List<String> splitSegments(String data) {
            StringBuilder current = new StringBuilder();
            java.util.ArrayList<String> segments = new java.util.ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                char ch = data.charAt(i);
                if (Character.isLetter(ch)) {
                    if (current.length() > 0) {
                        segments.add(current.toString().trim());
                    }
                    current = new StringBuilder();
                    current.append(ch);
                } else {
                    current.append(ch);
                }
            }
            if (current.length() > 0) {
                segments.add(current.toString().trim());
            }
            return segments;
        }

        private static double[] parseNumbers(String data) {
            if (data == null || data.isBlank()) {
                return new double[0];
            }
            Matcher matcher = NUMBER_PATTERN.matcher(data);
            java.util.ArrayList<Double> values = new java.util.ArrayList<>();
            while (matcher.find()) {
                String token = matcher.group();
                if (token == null || token.isBlank()) {
                    continue;
                }
                values.add(Double.parseDouble(token));
            }
            double[] result = new double[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }
    }
}
