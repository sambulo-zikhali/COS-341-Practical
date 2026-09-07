#ifndef CANVAS_H
#define CANVAS_H

#include "shape.h"

class Canvas {
    private:
        Shape* shapes;
    public:
        Memento* updateCurrent();
        void undoActionPrev(Memento* prev);
};


#endif