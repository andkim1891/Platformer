//package dev.lwjgl.ui.components;
//
//public class UIHeart extends UIComponent {
//    private String name;
//    public String getName() {
//        return this.name;
//    }
//
//    // optional: only if you want to change the name later
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    private float[] color;
//    private float alpha;
//
//    // ---------- ROTATION ----------
//    int rotationCounter;
//    boolean isRotating;
//
//    public void setRotating(boolean isRotating) {
//        this.isRotating = isRotating;
//        this.rotationCounter = 0;
//    }
//
//    // ---------- GROW ----------
//    boolean isGrowing;
//
//    public void setGrowing(boolean isGrowing) {
//        this.isGrowing = isGrowing;
//    }
//
//
//    // ---------- POLYGON DATA ----------
//    int n;
//    public double xCenter;
//    public double yCenter;
//    double radius;
//    double rotationalAngle;
//
//
//    public static UIHeart createHeart(String name, int n,double xCenter, double yCenter, double size, float alpha) {
//
//        UIHeart heart = new UIHeart(name, n, xCenter, yCenter, size, 0, alpha);
//
//        double[] xs = new double[n];
//        double[] ys = new double[n];
//
//        for (int i = 0; i < n; i++) {
//            double t = 2 * Math.PI * i / n;
//
//            double x = 16 * Math.pow(Math.sin(t), 3);
//            double y = 13 * Math.cos(t)
//                    - 5 * Math.cos(2 * t)
//                    - 2 * Math.cos(3 * t)
//                    - Math.cos(4 * t);
//
//            xs[i] = xCenter + x * size * 0.03;
//            ys[i] = yCenter + y * size * 0.03;
//        }
//
//        heart.setCustomVertices(xs, ys);
//        return heart;
//    }
//
//}