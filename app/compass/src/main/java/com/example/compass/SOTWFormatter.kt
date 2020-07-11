package com.example.compass

class SOTWFormatter {
    companion object {
        private val sides = intArrayOf(0, 45, 90, 135, 180, 225, 270, 315, 360)
        private val names = SOTW.values()

        fun format(azimuth: Float): Pair<Int, SOTW> {
            val iAzimuth = azimuth.toInt()
            var index = findClosestIndex(iAzimuth)

            if (index == 8) index = 0

            return iAzimuth to names[index]
        }

        /**
         * Finds index of the closest element to identify Side Of The World label
         *
         * @param target
         * @return index of the closest element
         */
        private fun findClosestIndex(target: Int): Int {
            // in the original binary search https://www.geeksforgeeks.org/find-closest-number-array/
            // you will see more steps to reduce the time
            // in in this particular case the corner conditions are never true
            // e.g. azimuth is never negative, so there is no point to check
            // these conditions. Also we don't check if target is equal to element of array,
            // because most of the time it's not.

            // and the main difference is it finds the index, not the value

            // Doing binary search
            var i = 0
            var j: Int = sides.size
            var mid = 0
            while (i < j) {
                mid = (i + j) / 2

                /* If target is less than array element,
                   then search in left */if (target < sides.get(mid)) {

                    // If target is greater than previous
                    // to mid, return closest of two
                    if (mid > 0 && target > sides.get(mid - 1)) {
                        return getClosest(mid - 1, mid, target)
                    }

                    /* Repeat for left half */j = mid
                } else {
                    if (mid < sides.size - 1 && target < sides.get(mid + 1)) {
                        return getClosest(mid, mid + 1, target)
                    }
                    i = mid + 1 // update i
                }
            }

            // Only single element left after search
            return mid
        }

        // Method to compare which one is the more close
        // We find the closest by taking the difference
        // between the target and both values. It assumes
        // that val2 is greater than val1 and target lies
        // between these two.
        private fun getClosest(index1: Int, index2: Int, target: Int): Int {
            return if (target - sides[index1] >= sides[index2] - target) {
                index2
            } else index1
        }
    }
}