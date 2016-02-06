package ws.palladian.extraction.location.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

public class GridCreatorTest {

    private final GeoCoordinate c1 = new ImmutableGeoCoordinate(-35.3, 149.116667); // canberra
    private final GeoCoordinate c2 = new ImmutableGeoCoordinate(35.683889, 139.774444); // tokyo
    private final GeoCoordinate c3 = new ImmutableGeoCoordinate(43.7, -79.4); // toronto
    private final GeoCoordinate c4 = new ImmutableGeoCoordinate(-22.908333, -43.196389); // rio de janeiro
    private final GeoCoordinate c5 = new ImmutableGeoCoordinate(21.311389, -157.796389); // hawaii
    private final GeoCoordinate c6 = new ImmutableGeoCoordinate(90, 180); // upper right corner
    private final GeoCoordinate c7 = new ImmutableGeoCoordinate(-90, -180); // lower left corner

    @SuppressWarnings("deprecation")
    @Test
    public void testGetCellIdentifier() {
        GridCreator gridCreator = new GridCreator(90);
        // System.out.println(gridCreator);

        assertEquals(8, gridCreator.getNumCells());
        assertEquals("(3|0)", gridCreator.getCellIdentifier(c1));
        assertEquals("(3|1)", gridCreator.getCellIdentifier(c2));
        assertEquals("(1|1)", gridCreator.getCellIdentifier(c3));
        assertEquals("(1|0)", gridCreator.getCellIdentifier(c4));
        assertEquals("(0|1)", gridCreator.getCellIdentifier(c5));
        assertEquals("(3|1)", gridCreator.getCellIdentifier(c6));
        assertEquals("(0|0)", gridCreator.getCellIdentifier(c7));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetCoordinate() {
        GridCreator gridCreator = new GridCreator(90);
        assertEquals(new ImmutableGeoCoordinate(-45, 135), gridCreator.getCoordinate("(3|0)"));
        assertEquals(new ImmutableGeoCoordinate(45, -135), gridCreator.getCoordinate("(0|1)"));
    }

    @Test
    public void testGetCell() {
        GridCreator gridCreator = new GridCreator(90);
        GridCell cell = gridCreator.getCell(c1);
        // System.out.println(cell);
        assertEquals(-90, cell.lat1, 0);
        assertEquals(0, cell.lat2, 0);
        assertEquals(90, cell.lng1, 0);
        assertEquals(180, cell.lng2, 0);
        assertEquals(new ImmutableGeoCoordinate(0, 180), cell.getNE());
        assertEquals(new ImmutableGeoCoordinate(-90, 180), cell.getSE());
        assertEquals(new ImmutableGeoCoordinate(0, 90), cell.getNW());
        assertEquals(new ImmutableGeoCoordinate(-90, 90), cell.getSW());
    }

    @Test
    public void testGetCells() {
        GridCell cell = new GridCreator(90).getCell(c1);
        List<GridCell> cells = new GridCreator(45).getCells(cell);
        assertEquals(4, cells.size());
        // CollectionHelper.print(cells);
        assertEquals(new ImmutableGeoCoordinate(-45, 90), cells.get(0).getNW());
        assertEquals(new ImmutableGeoCoordinate(-45, 135), cells.get(1).getNW());
        assertEquals(new ImmutableGeoCoordinate(0, 90), cells.get(2).getNW());
        assertEquals(new ImmutableGeoCoordinate(0, 135), cells.get(3).getNW());
        assertTrue(cell.intersects(cells.get(0)));
        assertTrue(cell.intersects(cells.get(1)));
        assertTrue(cell.intersects(cells.get(2)));
        assertTrue(cell.intersects(cells.get(3)));
        assertFalse(cell.intersects(new GridCreator(45).getCell(new ImmutableGeoCoordinate(45, 90))));
    }

    @Test
    public void testGetCellsIterator() {
        GridCreator gridCreator = new GridCreator(90);
        int numCellsViaIterator = CollectionHelper.count(gridCreator.iterator());
        int numCells = gridCreator.getNumCells();
        assertEquals(numCells, numCellsViaIterator);
    }

    @Test
    @Ignore
    public void testGetCellByIdentifier_performance() {
        StopWatch stopWatch = new StopWatch();
        GridCreator gridCreator = new GridCreator(2.5);
        for (int i = 0; i < 1000000; i++) {
            gridCreator.getCell("(23|45)");
        }
        System.out.println(stopWatch);
        // 2s:333ms
        // 564ms
    }

}
