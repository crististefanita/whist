package ro.cardtable.browser;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit geometry projection for static adapters, independent of any game's generated catalogue. */
final class BrowserLayoutJson {
    private BrowserLayoutJson() { }
    static Map<String,Object> fields(BrowserGeometry.Layout layout) {
        var fields = new LinkedHashMap<String,Object>();
        fields.put("board",layout.board()); fields.put("hand",layout.hand());
        fields.put("boardHeight",layout.boardHeight()); fields.put("minimumBoardHeight",layout.minimumBoardHeight());
        fields.put("played",layout.played()); fields.put("bids",layout.bids()); fields.put("playedStyle",layout.playedStyle());
        return fields;
    }
}
