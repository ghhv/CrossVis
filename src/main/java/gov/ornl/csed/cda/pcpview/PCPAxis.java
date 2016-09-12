package gov.ornl.csed.cda.pcpview;

import gov.ornl.csed.cda.datatable.Column;
import gov.ornl.csed.cda.datatable.ColumnSelectionRange;
import gov.ornl.csed.cda.datatable.DataModel;
import gov.ornl.csed.cda.datatable.Histogram;
import gov.ornl.csed.cda.util.GraphicsUtil;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by csg on 8/23/16.
 */
public class PCPAxis {
    public final static Logger log = LoggerFactory.getLogger(PCPAxis.class);

    public final static Paint DEFAULT_HISTOGRAM_FILL = new Color(Color.DARKGRAY.getRed(), Color.DARKGRAY.getGreen(), Color.DARKGRAY.getBlue(), 0.8d);
    public final static Paint DEFAULT_QUERY_HISTOGRAM_FILL = new Color(Color.STEELBLUE.getRed(), Color.STEELBLUE.getGreen(), Color.STEELBLUE.getBlue(), 0.8d);
    public final static Paint DEFAULT_HISTOGRAM_STROKE = Color.DARKGRAY;


    public final static double DEFAULT_NAME_LABEL_HEIGHT = 30d;
    public final static double DEFAULT_CONTEXT_HEIGHT = 20d;
    public final static double DEFAULT_BAR_WIDTH = 10d;
    public final static double DEFAULT_TEXT_SIZE = 10d;
    public final static double DEFAULT_STROKE_WIDTH = 1.5;

    private DataModel dataModel;
    private Column column;
    private int dataModelIndex;

    private double centerX;
    private Rectangle bounds;
    private double barTopY;
    private double barBottomY;
    private double focusTopY;
    private double focusBottomY;

    private double contextRegionHeight = DEFAULT_CONTEXT_HEIGHT;

    private Group graphicsGroup;
    private Line topCrossBarLine;
    private Line bottomCrossBarLine;
    private Line topFocusCrossBarLine;
    private Line bottomFocusCrossBarLine;

    private Rectangle verticalBar;

    private Text nameText;

    // histogram bin rectangles
    private Group histogramBinRectangleGroup;
    private ArrayList<Rectangle> histogramBinRectangleList;
    private Group queryHistogramBinRectangleGroup;
    private ArrayList<Rectangle> queryHistogramBinRectangleList;

    private Paint histogramFill = DEFAULT_HISTOGRAM_FILL;
    private Paint queryHistogramFill = DEFAULT_QUERY_HISTOGRAM_FILL;
    private Paint histogramStroke = DEFAULT_HISTOGRAM_STROKE;

    private Pane pane;

    private ArrayList<PCPAxisSelection> axisSelectionList = new ArrayList<>();

    // dragging variables
    Point2D dragStartPoint;
    Point2D dragEndPoint;
    PCPAxisSelection draggingSelection;
    boolean dragging = false;

    public PCPAxis(Column column, int dataModelIndex, DataModel dataModel, Pane pane) {
        this.column = column;
        this.dataModelIndex = dataModelIndex;
        this.dataModel = dataModel;
        this.pane = pane;

        centerX = 0d;
        bounds = new Rectangle();
        barTopY = 0d;
        barBottomY = 0d;
        focusTopY = 0d;
        focusBottomY = 0d;

        nameText = new Text(column.getName());
        nameText.setFont(new Font(DEFAULT_TEXT_SIZE));

        verticalBar = new Rectangle();
        verticalBar.setStroke(Color.DARKGRAY);
        verticalBar.setFill(Color.WHITESMOKE);
        verticalBar.setSmooth(true);
        verticalBar.setStrokeWidth(DEFAULT_STROKE_WIDTH);

        topCrossBarLine = makeLine();
        bottomCrossBarLine = makeLine();
        topFocusCrossBarLine = makeLine();
        bottomFocusCrossBarLine = makeLine();

        graphicsGroup = new Group(verticalBar, topCrossBarLine, bottomCrossBarLine, topFocusCrossBarLine, bottomFocusCrossBarLine, nameText);

        registerListeners();
    }

    public Group getHistogramBinRectangleGroup() { return histogramBinRectangleGroup; }
    public Group getQueryHistogramBinRectangleGroup() { return queryHistogramBinRectangleGroup; }

    private void registerListeners() {
        PCPAxis thisPCPAxis = this;

        verticalBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dragStartPoint = new Point2D(event.getX(), event.getY());
                dragEndPoint = new Point2D(event.getX(), event.getY());
            }
        });

        verticalBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                dragging = true;
                dragEndPoint = new Point2D(event.getX(), event.getY());

                double selectionMaxY = Math.min(dragStartPoint.getY(), dragEndPoint.getY());
                double selectionMinY = Math.max(dragStartPoint.getY(), dragEndPoint.getY());

                selectionMaxY = selectionMaxY < getFocusTopY() ? getFocusTopY() : selectionMaxY;
                selectionMinY = selectionMinY > getFocusBottomY() ? getFocusBottomY() : selectionMinY;

                double maxSelectionValue = GraphicsUtil.mapValue(selectionMaxY, getFocusTopY(), getFocusBottomY(),
                        column.getSummaryStats().getMax(), column.getSummaryStats().getMin());
                double minSelectionValue = GraphicsUtil.mapValue(selectionMinY, getFocusTopY(), getFocusBottomY(),
                        column.getSummaryStats().getMax(), column.getSummaryStats().getMin());

                if (draggingSelection == null) {
                    ColumnSelectionRange selectionRange = dataModel.addColumnSelectionRangeToActiveQuery(column, (float)minSelectionValue, (float)maxSelectionValue);
                    draggingSelection = new PCPAxisSelection(thisPCPAxis, selectionRange, selectionMinY, selectionMaxY, pane, dataModel);
                } else {
                    draggingSelection.update(minSelectionValue, maxSelectionValue, selectionMinY, selectionMaxY);
                }

            }
        });

        verticalBar.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (draggingSelection != null) {
                    axisSelectionList.add(draggingSelection);
                    dragging = false;
                    draggingSelection = null;
                    dataModel.setQueriedTuples();
                }
            }
        });
    }

    public ArrayList<PCPAxisSelection> getAxisSelectionList() { return axisSelectionList; }

    private Line makeLine() {
        Line line = new Line();
        line.setStroke(Color.DARKGRAY);
        line.setSmooth(true);
        line.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        return line;
    }

    public Group getGraphicsGroup() { return graphicsGroup; }

    public Text getNameText() { return nameText; }

    public void layout(double centerX, double topY, double width, double height) {
        this.centerX = centerX;
        double left = centerX - (width / 2.);
        bounds = new Rectangle(left, topY, width, height);
        barTopY = topY + DEFAULT_NAME_LABEL_HEIGHT;
        barBottomY = bounds.getY() + bounds.getHeight();
        focusTopY = topY + DEFAULT_NAME_LABEL_HEIGHT + contextRegionHeight;
        focusBottomY = barBottomY - contextRegionHeight;

        verticalBar.setX(centerX - (DEFAULT_BAR_WIDTH / 2.));
        verticalBar.setY(barTopY);
        verticalBar.setWidth(DEFAULT_BAR_WIDTH);
        verticalBar.setHeight(barBottomY - barTopY);

        topCrossBarLine.setStartY(barTopY);
        topCrossBarLine.setEndY(barTopY);
        topCrossBarLine.setStartX(centerX - (DEFAULT_BAR_WIDTH / 2.));
        topCrossBarLine.setEndX(centerX + (DEFAULT_BAR_WIDTH / 2.));

        bottomCrossBarLine.setStartY(barBottomY);
        bottomCrossBarLine.setEndY(barBottomY);
        bottomCrossBarLine.setStartX(centerX - (DEFAULT_BAR_WIDTH / 2.));
        bottomCrossBarLine.setEndX(centerX + (DEFAULT_BAR_WIDTH / 2.));

        topFocusCrossBarLine.setStartY(focusTopY);
        topFocusCrossBarLine.setEndY(focusTopY);
        topFocusCrossBarLine.setStartX(centerX - (DEFAULT_BAR_WIDTH / 2.));
        topFocusCrossBarLine.setEndX(centerX + (DEFAULT_BAR_WIDTH / 2.));

        bottomFocusCrossBarLine.setStartY(focusBottomY);
        bottomFocusCrossBarLine.setEndY(focusBottomY);
        bottomFocusCrossBarLine.setStartX(centerX - (DEFAULT_BAR_WIDTH / 2.));
        bottomFocusCrossBarLine.setEndX(centerX + (DEFAULT_BAR_WIDTH / 2.));

        nameText.setFont(new Font(DEFAULT_TEXT_SIZE));
//        adjustTextSize(nameText, width, DEFAULT_TEXT_SIZE);
        nameText.setX(bounds.getX() + ((width - nameText.getLayoutBounds().getWidth()) / 2.));
        nameText.setY(barTopY - (DEFAULT_NAME_LABEL_HEIGHT / 2.));
        nameText.setRotate(-10.);

        if (!axisSelectionList.isEmpty()) {
            for (PCPAxisSelection pcpAxisSelection : axisSelectionList) {
                pcpAxisSelection.relayout();
            }
        }

        if (!dataModel.isEmpty()) {
            Histogram histogram = column.getSummaryStats().getHistogram();

            double binHeight = (getFocusBottomY() - getFocusTopY()) / histogram.getNumBins();
            histogramBinRectangleList = new ArrayList<>();

            if (histogramBinRectangleGroup != null) {
                pane.getChildren().remove(histogramBinRectangleGroup);
            }

            histogramBinRectangleGroup = new Group();

            for (int i = 0; i < histogram.getNumBins(); i++) {
                double y = getFocusTopY() + ((histogram.getNumBins() - i - 1) * binHeight);
                double binWidth = GraphicsUtil.mapValue(histogram.getBinCount(i), 0, histogram.getMaxBinCount(), DEFAULT_BAR_WIDTH + 2, width - 2);
                double x = left + ((width - binWidth) / 2.);
                Rectangle rectangle = new Rectangle(x, y, binWidth, binHeight);
                rectangle.setStroke(histogramStroke);
                rectangle.setFill(histogramFill);
                histogramBinRectangleList.add(rectangle);
                histogramBinRectangleGroup.getChildren().add(rectangle);
            }

            queryHistogramBinRectangleList = new ArrayList<>();
            if (queryHistogramBinRectangleGroup != null) {
                pane.getChildren().remove(queryHistogramBinRectangleGroup);
            }
            queryHistogramBinRectangleGroup = new Group();

            if (dataModel.getActiveQuery().hasColumnSelections()) {
                Histogram queryHistogram = dataModel.getActiveQuery().getColumnQuerySummaryStats(column).getHistogram();

                for (int i = 0; i < histogram.getNumBins(); i++) {
                    if (queryHistogram.getBinCount(i) > 0) {
                        double y = getFocusTopY() + ((histogram.getNumBins() - i - 1) * binHeight);
                        double binWidth = GraphicsUtil.mapValue(queryHistogram.getBinCount(i), 0, histogram.getMaxBinCount(), DEFAULT_BAR_WIDTH + 2, width - 2);
                        double x = left + ((width - binWidth) / 2.);
                        Rectangle rectangle = new Rectangle(x, y, binWidth, binHeight);
                        rectangle.setStroke(histogramStroke);
                        rectangle.setFill(queryHistogramFill);
                        queryHistogramBinRectangleList.add(rectangle);
                        queryHistogramBinRectangleGroup.getChildren().add(rectangle);
                    }
                }
            }
        }
    }

    public double getBarLeftX() { return verticalBar.getX(); }
    public double getBarRightX() { return verticalBar.getX() + verticalBar.getWidth(); }

    public double getCenterX() { return centerX; }
    public Rectangle getBounds() { return bounds; }

    public Rectangle getVerticalBar() { return verticalBar; }

    public Column getColumn() { return column; }
    public int getColumnDataModelIndex() { return dataModelIndex; }

    public double getFocusTopY() { return focusTopY; }
    public double getFocusBottomY() { return focusBottomY; }
    public double getUpperContextTopY() { return barTopY; }
    public double getUpperContextBottomY() { return focusTopY; }
    public double getLowerContextTopY() { return focusBottomY; }
    public double getLowerContextBottomY() { return barBottomY; }

    public double getVerticalBarTop() { return barTopY; }
    public double getVerticalBarBottom() { return barBottomY; }

    private void adjustTextSize(Text text, double maxWidth, double fontSize) {
        String fontName = text.getFont().getName();
        while (text.getLayoutBounds().getWidth() > maxWidth && fontSize > 0) {
            fontSize -= 0.005;
            text.setFont(new Font(fontName, fontSize));
        }
    }
}
